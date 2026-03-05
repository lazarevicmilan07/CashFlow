package com.moneytracker.simplebudget.domain.usecase

import android.content.Context
import android.net.Uri
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.AccountRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import com.moneytracker.simplebudget.domain.model.Expense
import com.moneytracker.simplebudget.ui.components.formatCurrency
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.Workbook
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

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
        val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault())
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
        return expenses.filter { expense ->
            !expense.date.isBefore(startDate) && !expense.date.isAfter(endDate)
        }
    }

    suspend fun exportToExcel(context: Context, uri: Uri, period: ExportPeriodParams): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val allExpenses = expenseRepository.getAllExpensesSync()
            val expenses = filterExpensesByPeriod(allExpenses, period)
            val categories = categoryRepository.getAllCategories().first()
            val accounts = accountRepository.getAllAccounts().first()
            val currency = preferencesManager.currency.first()

            val categoriesMap = categories.associateBy { it.id }
            val accountsMap = accounts.associateBy { it.id }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                Workbook(outputStream, "MoneyTracker", "1.0").use { workbook ->
                    val sheet = workbook.newWorksheet("Expenses")

                    // Header row
                    val headers = listOf("Date", "Type", "Category", "Subcategory", "Account", "Amount", "Currency", "Note")
                    headers.forEachIndexed { col, header ->
                        sheet.value(0, col, header)
                        sheet.style(0, col).bold().set()
                    }

                    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                    // Data rows
                    expenses.forEachIndexed { index, expense ->
                        val row = index + 1
                        val categoryName = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                        val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""
                        val accountName = expense.accountId?.let { accountsMap[it]?.name } ?: ""

                        sheet.value(row, 0, expense.date.format(dateFormatter))
                        sheet.value(row, 1, expense.type.name)
                        sheet.value(row, 2, categoryName)
                        sheet.value(row, 3, subcategoryName)
                        sheet.value(row, 4, accountName)
                        sheet.value(row, 5, expense.amount)
                        sheet.value(row, 6, currency)
                        sheet.value(row, 7, expense.note)
                    }

                    // Set column widths
                    sheet.width(0, 12.0)  // Date
                    sheet.width(1, 10.0)  // Type
                    sheet.width(2, 15.0)  // Category
                    sheet.width(3, 15.0)  // Subcategory
                    sheet.width(4, 15.0)  // Account
                    sheet.width(5, 12.0)  // Amount
                    sheet.width(6, 10.0)  // Currency
                    sheet.width(7, 30.0)  // Note
                }
            }
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

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val pdfWriter = PdfWriter(outputStream)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                // Title
                document.add(
                    Paragraph("Expense Report")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(8f)
                )

                // Period subtitle
                document.add(
                    Paragraph(period.getPeriodTitle())
                        .setFontSize(14f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20f)
                )

                // Summary
                val totalIncome =
                    expenses.filter { it.type == com.moneytracker.simplebudget.domain.model.TransactionType.INCOME }
                        .sumOf { it.amount }
                val totalExpense =
                    expenses.filter { it.type == com.moneytracker.simplebudget.domain.model.TransactionType.EXPENSE }
                        .sumOf { it.amount }

                document.add(
                    Paragraph("Total Income: ${formatCurrency(totalIncome, currency, symbolAfter)}").setFontSize(12f)
                )
                document.add(
                    Paragraph("Total Expenses: ${formatCurrency(totalExpense, currency, symbolAfter)}").setFontSize(12f)
                )
                document.add(
                    Paragraph("Balance: ${formatCurrency(totalIncome - totalExpense, currency, symbolAfter)}")
                        .setFontSize(12f).setMarginBottom(20f)
                )

                // Table with 7 columns
                val table =
                    Table(UnitValue.createPercentArray(floatArrayOf(1.3f, 0.9f, 1.3f, 1.3f, 1.2f, 1f, 2f)))
                        .useAllAvailableWidth()

                // Header
                listOf("Date", "Type", "Category", "Subcategory", "Account", "Amount", "Note").forEach { header ->
                    table.addHeaderCell(
                        Cell().add(Paragraph(header).setBold())
                            .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    )
                }

                val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

                expenses.forEach { expense ->
                    val categoryName = expense.categoryId?.let { categoriesMap[it]?.name } ?: ""
                    val subcategoryName = expense.subcategoryId?.let { categoriesMap[it]?.name } ?: ""
                    val accountName = expense.accountId?.let { accountsMap[it]?.name } ?: ""

                    table.addCell(expense.date.format(dateFormatter))
                    table.addCell(expense.type.name)
                    table.addCell(categoryName)
                    table.addCell(subcategoryName)
                    table.addCell(accountName)
                    table.addCell(formatCurrency(expense.amount, currency, symbolAfter))
                    table.addCell(expense.note)
                }

                document.add(table)
                document.close()
            }
        }
    }
}
