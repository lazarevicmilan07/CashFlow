package com.moneytracker.simplebudget.domain.usecase

import android.content.Context
import android.net.Uri
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.AccountRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import com.moneytracker.simplebudget.domain.model.Expense
import com.moneytracker.simplebudget.domain.model.TransactionType
import com.moneytracker.simplebudget.ui.components.formatCurrency
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

// ─── PDF color palette ────────────────────────────────────────────────────────

private val PDF_NAVY         = DeviceRgb(0x1A, 0x37, 0x5E)   // header background
private val PDF_PRIMARY      = DeviceRgb(0x1A, 0x73, 0xE8)   // accent / table header
private val PDF_SUBTITLE     = DeviceRgb(0x8A, 0xB4, 0xF8)   // header subtitle
private val PDF_GREEN        = DeviceRgb(0x2E, 0x7D, 0x32)   // income text
private val PDF_GREEN_TINT   = DeviceRgb(0xE8, 0xF5, 0xE9)   // income card background
private val PDF_RED          = DeviceRgb(0xC6, 0x28, 0x28)   // expense text
private val PDF_RED_TINT     = DeviceRgb(0xFF, 0xEB, 0xEE)   // expense card background
private val PDF_AMBER        = DeviceRgb(0xFF, 0x8F, 0x00)   // balance text
private val PDF_AMBER_TINT   = DeviceRgb(0xFF, 0xF8, 0xE1)   // balance card background
private val PDF_PRIMARY_TINT = DeviceRgb(0xE3, 0xF2, 0xFD)   // count card background
private val PDF_STRIPE       = DeviceRgb(0xEC, 0xEF, 0xF4)   // alternating row
private val PDF_BORDER       = DeviceRgb(0xCC, 0xCC, 0xCC)   // row separator
private val PDF_WHITE        = DeviceRgb(0xFF, 0xFF, 0xFF)
private val PDF_TEXT         = DeviceRgb(0x1E, 0x29, 0x3B)   // body text
private val PDF_LABEL        = DeviceRgb(0x70, 0x70, 0x70)   // card label

// ─── Excel color palette (raw bytes for XSSFColor) ───────────────────────────

private val XL_NAVY  = byteArrayOf(0x1A.toByte(), 0x37.toByte(), 0x5E.toByte())
private val XL_BLUE  = byteArrayOf(0x1A.toByte(), 0x73.toByte(), 0xE8.toByte())
private val XL_BLUET = byteArrayOf(0xE3.toByte(), 0xF2.toByte(), 0xFD.toByte())
private val XL_GRAY  = byteArrayOf(0xCC.toByte(), 0xCC.toByte(), 0xCC.toByte())
private val XL_GREEN = byteArrayOf(0x2E.toByte(), 0x7D.toByte(), 0x32.toByte())
private val XL_RED   = byteArrayOf(0xC6.toByte(), 0x28.toByte(), 0x28.toByte())
private val XL_SLATE = byteArrayOf(0xEC.toByte(), 0xEF.toByte(), 0xF4.toByte())
private val XL_LGRAY = byteArrayOf(0xF5.toByte(), 0xF5.toByte(), 0xF5.toByte())
private val XL_WHITE = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

// ─────────────────────────────────────────────────────────────────────────────

data class ExportPeriodParams(
    val year: Int,
    val month: Int? = null // null means entire year
) {
    fun getStartDate(): LocalDate = if (month != null) {
        LocalDate.of(year, month, 1)
    } else {
        LocalDate.of(year, 1, 1)
    }

    fun getEndDate(): LocalDate = if (month != null) {
        LocalDate.of(year, month, 1).plusMonths(1).minusDays(1)
    } else {
        LocalDate.of(year, 12, 31)
    }

    fun getPeriodTitle(): String = if (month != null) {
        val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        "$monthName $year"
    } else {
        "Year $year"
    }
}

@Singleton
class ExportUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager
) {

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: ExportPeriodParams): List<Expense> {
        val startDate = period.getStartDate()
        val endDate = period.getEndDate()
        return expenses.filter { !it.date.isBefore(startDate) && !it.date.isAfter(endDate) }
    }

    // ─── Excel ───────────────────────────────────────────────────────────────

    private data class XlStyles(
        val title: XSSFCellStyle,
        val section: XSSFCellStyle,
        val summaryHeader: XSSFCellStyle,
        val summaryLabel: XSSFCellStyle,
        val tblHeader: XSSFCellStyle,
        val dataNormal: XSSFCellStyle,
        val dataAlt: XSSFCellStyle,
        val income: XSSFCellStyle,
        val incomeAlt: XSSFCellStyle,
        val expense: XSSFCellStyle,
        val expenseAlt: XSSFCellStyle
    )

    private fun buildXlStyles(wb: XSSFWorkbook): XlStyles {
        fun xColor(bytes: ByteArray) = XSSFColor(bytes, null)

        fun boldFont(size: Short, colorBytes: ByteArray): XSSFFont =
            (wb.createFont() as XSSFFont).apply {
                bold = true
                fontHeightInPoints = size
                setColor(xColor(colorBytes))
            }

        fun baseStyle() = wb.createCellStyle().apply {
            setVerticalAlignment(VerticalAlignment.CENTER)
        }

        val title = baseStyle().apply {
            setFillForegroundColor(xColor(XL_NAVY))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.CENTER)
            setFont(boldFont(15, XL_WHITE))
        }

        val section = baseStyle().apply {
            setFillForegroundColor(xColor(XL_BLUE))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.LEFT)
            setFont(boldFont(11, XL_WHITE))
            setBorderBottom(BorderStyle.THIN)
            setBottomBorderColor(xColor(XL_BLUET))
        }

        // Light summary header: tinted background, dark navy text (no heavy blue fill)
        val summaryHeader = baseStyle().apply {
            setFillForegroundColor(xColor(XL_BLUET))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.LEFT)
            setFont(boldFont(11, XL_NAVY))
            setBorderBottom(BorderStyle.MEDIUM)
            setBottomBorderColor(xColor(XL_BLUE))
        }

        // Light summary label cells: very light gray, dark text, indented
        val summaryLabel = baseStyle().apply {
            setFillForegroundColor(xColor(XL_LGRAY))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.LEFT)
            val f = wb.createFont().apply {
                fontHeightInPoints = 10
                bold = true
                (this as XSSFFont).setColor(xColor(XL_NAVY))
            }
            setFont(f)
            setBorderBottom(BorderStyle.HAIR)
            setBottomBorderColor(xColor(XL_GRAY))
            indention = 1
        }

        val tblHeader = baseStyle().apply {
            setFillForegroundColor(xColor(XL_BLUET))
            fillPattern = FillPatternType.SOLID_FOREGROUND
            setAlignment(HorizontalAlignment.CENTER)
            setFont(boldFont(10, XL_BLUE))
            setBorderTop(BorderStyle.THIN); setTopBorderColor(xColor(XL_BLUE))
            setBorderBottom(BorderStyle.MEDIUM); setBottomBorderColor(xColor(XL_BLUE))
            setBorderLeft(BorderStyle.THIN); setLeftBorderColor(xColor(XL_GRAY))
            setBorderRight(BorderStyle.THIN); setRightBorderColor(xColor(XL_GRAY))
        }

        val dataNormal = baseStyle().apply {
            setAlignment(HorizontalAlignment.LEFT)
            setBorderBottom(BorderStyle.HAIR)
            setBottomBorderColor(xColor(XL_GRAY))
            val f = wb.createFont().apply { fontHeightInPoints = 9 }
            setFont(f)
        }

        val dataAlt = wb.createCellStyle().apply {
            cloneStyleFrom(dataNormal)
            setFillForegroundColor(xColor(XL_SLATE))
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val income = wb.createCellStyle().apply {
            cloneStyleFrom(dataNormal)
            setFont(boldFont(9, XL_GREEN))
        }
        val incomeAlt = wb.createCellStyle().apply {
            cloneStyleFrom(income)
            setFillForegroundColor(xColor(XL_SLATE))
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val expense = wb.createCellStyle().apply {
            cloneStyleFrom(dataNormal)
            setFont(boldFont(9, XL_RED))
        }
        val expenseAlt = wb.createCellStyle().apply {
            cloneStyleFrom(expense)
            setFillForegroundColor(xColor(XL_SLATE))
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        return XlStyles(title, section, summaryHeader, summaryLabel, tblHeader, dataNormal, dataAlt, income, incomeAlt, expense, expenseAlt)
    }

    private fun xlMergedRow(
        sheet: org.apache.poi.xssf.usermodel.XSSFSheet,
        rowIdx: Int,
        colCount: Int,
        text: String,
        style: XSSFCellStyle,
        heightPt: Float = 24f
    ) {
        val row = sheet.createRow(rowIdx).apply { heightInPoints = heightPt }
        row.createCell(0).apply { setCellValue(text); cellStyle = style }
        for (c in 1 until colCount) row.createCell(c).cellStyle = style
        if (colCount > 1) sheet.addMergedRegion(CellRangeAddress(rowIdx, rowIdx, 0, colCount - 1))
    }

    suspend fun exportToExcel(context: Context, uri: Uri, period: ExportPeriodParams): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val allExpenses = expenseRepository.getAllExpensesSync()
            val expenses = filterExpensesByPeriod(allExpenses, period)
            val categories = categoryRepository.getAllCategories().first()
            val accounts = accountRepository.getAllAccounts().first()
            val currency = preferencesManager.currency.first()
            val symbolAfter = preferencesManager.currencySymbolAfter.first()

            val categoriesMap = categories.associateBy { it.id }
            val accountsMap = accounts.associateBy { it.id }

            val totalIncome = expenses.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = expenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpense

            val workbook = XSSFWorkbook()
            val s = buildXlStyles(workbook)
            val sheet = workbook.createSheet("Expenses")
            val COL = 8
            var r = 0

            // Title
            xlMergedRow(sheet, r++, COL, "CashFlow  ·  ${period.getPeriodTitle()}", s.title, 36f)
            sheet.createRow(r++) // spacer

            // Summary
            xlMergedRow(sheet, r++, COL, "SUMMARY", s.summaryHeader, 22f)
            listOf(
                Triple("Total Income",   formatCurrency(totalIncome,  currency, symbolAfter), s.income),
                Triple("Total Expenses", formatCurrency(totalExpense, currency, symbolAfter), s.expense),
                Triple("Net Balance",    formatCurrency(netBalance,   currency, symbolAfter), if (netBalance >= 0) s.income else s.expense),
                Triple("Transactions",   expenses.size.toString(),                             s.dataNormal)
            ).forEach { (label, value, valueStyle) ->
                sheet.createRow(r++).apply {
                    heightInPoints = 18f
                    createCell(0).apply { setCellValue(label); cellStyle = s.summaryLabel }
                    createCell(1).apply { setCellValue(value); cellStyle = valueStyle }
                    for (c in 2 until COL) createCell(c).cellStyle = s.summaryLabel
                }
            }
            sheet.createRow(r++) // spacer

            // Transactions
            xlMergedRow(sheet, r++, COL, "TRANSACTIONS", s.section, 22f)
            sheet.createRow(r++).apply {
                heightInPoints = 20f
                listOf("Date", "Type", "Category", "Subcategory", "Account", "Amount", "Currency", "Note")
                    .forEachIndexed { c, h -> createCell(c).apply { setCellValue(h); cellStyle = s.tblHeader } }
            }

            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

            expenses.forEachIndexed { i, expense ->
                val alt = i % 2 == 1
                val baseStyle = if (alt) s.dataAlt else s.dataNormal
                val amtStyle = when (expense.type) {
                    TransactionType.INCOME   -> if (alt) s.incomeAlt  else s.income
                    TransactionType.EXPENSE  -> if (alt) s.expenseAlt else s.expense
                    else                     -> baseStyle
                }
                val categoryName    = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""
                val accountName     = expense.accountId?.let { accountsMap[it]?.name } ?: ""

                sheet.createRow(r++).apply {
                    heightInPoints = 18f
                    createCell(0).apply { setCellValue(expense.date.format(dateFormatter));                    cellStyle = baseStyle }
                    createCell(1).apply { setCellValue(expense.type.name);                                    cellStyle = baseStyle }
                    createCell(2).apply { setCellValue(categoryName);                                          cellStyle = baseStyle }
                    createCell(3).apply { setCellValue(subcategoryName);                                       cellStyle = baseStyle }
                    createCell(4).apply { setCellValue(accountName);                                           cellStyle = baseStyle }
                    createCell(5).apply { setCellValue(formatCurrency(expense.amount, currency, symbolAfter)); cellStyle = amtStyle  }
                    createCell(6).apply { setCellValue(currency);                                              cellStyle = baseStyle }
                    createCell(7).apply { setCellValue(expense.note);                                          cellStyle = baseStyle }
                }
            }

            // Column widths (units: 1/256th of character width)
            sheet.setColumnWidth(0, 22 * 256)  // wider for summary labels ("Total Expenses")
            sheet.setColumnWidth(1, 18 * 256)  // wider for formatted currency amounts
            sheet.setColumnWidth(2, 18 * 256)
            sheet.setColumnWidth(3, 18 * 256)
            sheet.setColumnWidth(4, 18 * 256)
            sheet.setColumnWidth(5, 16 * 256)
            sheet.setColumnWidth(6, 10 * 256)
            sheet.setColumnWidth(7, 35 * 256)

            context.contentResolver.openOutputStream(uri)?.use { workbook.write(it) }
            workbook.close()
        }
    }

    // ─── PDF ─────────────────────────────────────────────────────────────────

    /** Full-width dark header with title and period subtitle. */
    private fun pdfHeader(doc: Document, title: String, subtitle: String) {
        val tbl = Table(UnitValue.createPercentArray(floatArrayOf(1f))).useAllAvailableWidth()
        val cell = Cell()
            .setBackgroundColor(PDF_NAVY)
            .setBorder(Border.NO_BORDER)
            .setPaddingTop(20f).setPaddingBottom(20f)
            .setPaddingLeft(16f).setPaddingRight(16f)
        cell.add(
            Paragraph(title)
                .setFontSize(22f).setBold()
                .setFontColor(PDF_WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(0f)
        )
        cell.add(
            Paragraph(subtitle)
                .setFontSize(12f)
                .setFontColor(PDF_SUBTITLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(4f).setMarginBottom(0f)
        )
        tbl.addCell(cell)
        doc.add(tbl)
    }

    /** 4 colored metric cards in a row: income, expense, balance, count. */
    private fun pdfMetricCards(
        doc: Document,
        income: Double, expense: Double, balance: Double, count: Int,
        currency: String, symbolAfter: Boolean
    ) {
        val tbl = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
            .useAllAvailableWidth()
            .setMarginTop(14f).setMarginBottom(14f)

        fun addCard(label: String, value: String, textColor: DeviceRgb, bgColor: DeviceRgb) {
            val cell = Cell()
                .setBackgroundColor(bgColor)
                .setBorder(Border.NO_BORDER)
                .setPadding(10f)
            cell.add(
                Paragraph(label)
                    .setFontSize(8f)
                    .setFontColor(PDF_LABEL)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(3f)
            )
            cell.add(
                Paragraph(value)
                    .setFontSize(13f).setBold()
                    .setFontColor(textColor)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            tbl.addCell(cell)
        }

        addCard("TOTAL INCOME",   formatCurrency(income,  currency, symbolAfter), PDF_GREEN,   PDF_GREEN_TINT)
        addCard("TOTAL EXPENSES", formatCurrency(expense, currency, symbolAfter), PDF_RED,     PDF_RED_TINT)
        addCard("NET BALANCE",    formatCurrency(balance, currency, symbolAfter), PDF_AMBER,   PDF_AMBER_TINT)
        addCard("TRANSACTIONS",   count.toString(),                                PDF_PRIMARY, PDF_PRIMARY_TINT)
        doc.add(tbl)
    }

    /** Section heading with a left accent stripe and light bottom border. */
    private fun pdfSectionTitle(doc: Document, text: String) {
        val tbl = Table(UnitValue.createPercentArray(floatArrayOf(1.5f, 98.5f)))
            .useAllAvailableWidth()
            .setMarginTop(18f).setMarginBottom(6f)
        tbl.addCell(
            Cell()
                .setBackgroundColor(PDF_PRIMARY)
                .setBorder(Border.NO_BORDER)
                .setMinHeight(22f)
        )
        tbl.addCell(
            Cell()
                .add(Paragraph(text).setFontSize(11f).setBold().setFontColor(PDF_TEXT))
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(SolidBorder(PDF_BORDER, 0.5f))
                .setPaddingLeft(8f).setPaddingTop(5f).setPaddingBottom(5f)
        )
        doc.add(tbl)
    }

    /** Blue header row for the transaction table. */
    private fun pdfTblHeader(table: Table, vararg headers: String) {
        headers.forEach { header ->
            table.addHeaderCell(
                Cell()
                    .add(
                        Paragraph(header)
                            .setFontSize(9f).setBold()
                            .setFontColor(PDF_WHITE)
                            .setTextAlignment(TextAlignment.CENTER)
                    )
                    .setBackgroundColor(PDF_PRIMARY)
                    .setBorder(Border.NO_BORDER)
                    .setPadding(6f)
            )
        }
    }

    /** Alternating-stripe data row with color-coded amount column. */
    private fun pdfDataRow(
        table: Table,
        values: List<String>,
        rowIndex: Int,
        amountIndex: Int,
        type: TransactionType
    ) {
        val bg = if (rowIndex % 2 == 1) PDF_STRIPE else null
        val amountColor = when (type) {
            TransactionType.INCOME   -> PDF_GREEN
            TransactionType.EXPENSE  -> PDF_RED
            else                     -> PDF_PRIMARY
        }
        values.forEachIndexed { colIdx, value ->
            val p = Paragraph(value).setFontSize(8.5f)
            if (colIdx == amountIndex) {
                p.setFontColor(amountColor).setBold()
            } else {
                p.setFontColor(PDF_TEXT)
            }
            val cell = Cell()
                .add(p)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(SolidBorder(PDF_BORDER, 0.3f))
                .setPaddingTop(5f).setPaddingBottom(5f)
                .setPaddingLeft(4f).setPaddingRight(4f)
            if (bg != null) cell.setBackgroundColor(bg)
            table.addCell(cell)
        }
    }

    suspend fun exportToPdf(context: Context, uri: Uri, period: ExportPeriodParams): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val allExpenses = expenseRepository.getAllExpensesSync()
            val expenses = filterExpensesByPeriod(allExpenses, period)
            val categories = categoryRepository.getAllCategories().first()
            val accounts = accountRepository.getAllAccounts().first()
            val currency = preferencesManager.currency.first()
            val symbolAfter = preferencesManager.currencySymbolAfter.first()

            val categoriesMap = categories.associateBy { it.id }
            val accountsMap = accounts.associateBy { it.id }

            val totalIncome  = expenses.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = expenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val netBalance   = totalIncome - totalExpense

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)
                document.setMargins(0f, 0f, 36f, 0f) // no top margin — header bleeds to edge

                pdfHeader(document, "Expense Report", period.getPeriodTitle())

                // Add side margins after the header
                document.setLeftMargin(36f)
                document.setRightMargin(36f)

                pdfMetricCards(document, totalIncome, totalExpense, netBalance, expenses.size, currency, symbolAfter)
                pdfSectionTitle(document, "Transactions")

                val table = Table(
                    UnitValue.createPercentArray(floatArrayOf(1.4f, 0.9f, 1.3f, 1.3f, 1.2f, 1.1f, 2f))
                ).useAllAvailableWidth()

                pdfTblHeader(table, "Date", "Type", "Category", "Subcategory", "Account", "Amount", "Note")

                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)

                expenses.forEachIndexed { index, expense ->
                    val categoryName    = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                    val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""
                    val accountName     = expense.accountId?.let { accountsMap[it]?.name } ?: ""

                    pdfDataRow(
                        table,
                        listOf(
                            expense.date.format(dateFormatter),
                            expense.type.name,
                            categoryName,
                            subcategoryName,
                            accountName,
                            formatCurrency(expense.amount, currency, symbolAfter),
                            expense.note
                        ),
                        index,
                        amountIndex = 5,
                        type = expense.type
                    )
                }

                document.add(table)
                document.close()
            }
        }
    }
}
