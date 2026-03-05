package com.moneytracker.simplebudget.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.moneytracker.simplebudget.domain.model.AccountType
import com.moneytracker.simplebudget.domain.model.TransactionType
import com.moneytracker.simplebudget.data.repository.AccountRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import com.moneytracker.simplebudget.domain.model.Account
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.Expense
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRestoreUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private fun filterExpensesByPeriod(expenses: List<Expense>, period: ExportPeriodParams): List<Expense> {
        val startDate = period.getStartDate()
        val endDate = period.getEndDate()
        return expenses.filter { expense ->
            !expense.date.isBefore(startDate) && !expense.date.isAfter(endDate)
        }
    }

    suspend fun backup(context: Context, uri: Uri, period: ExportPeriodParams): Result<Unit> = runCatching {
        val allExpenses = expenseRepository.getAllExpensesSync()
        val expenses = filterExpensesByPeriod(allExpenses, period)
        val categories = categoryRepository.getAllCategoriesSync()
        val accounts = accountRepository.getAllAccountsSync()

        val backupData = BackupData(
            version = BACKUP_VERSION,
            timestamp = System.currentTimeMillis(),
            periodYear = period.year,
            periodMonth = period.month,
            expenses = expenses.map { it.toBackupExpense() },
            categories = categories.map { it.toBackupCategory() },
            accounts = accounts.map { it.toBackupAccount() }
        )

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(json.encodeToString(backupData))
            }
        }
    }

    suspend fun restore(context: Context, uri: Uri): Result<Unit> = runCatching {
        val backupData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                val content = reader.readText()
                json.decodeFromString<BackupData>(content)
            }
        } ?: throw IllegalStateException("Could not read backup file")

        // Clear ALL existing data (expenses first due to foreign key-like references)
        expenseRepository.deleteAllExpenses()
        categoryRepository.deleteAllCategories()
        accountRepository.deleteAllAccounts()

        // Restore accounts with ID mapping
        val accountIdMapping = mutableMapOf<Long, Long>()
        backupData.accounts.forEach { backupAccount ->
            val account = backupAccount.toAccount()
            val newId = accountRepository.insertAccount(account.copy(id = 0))
            accountIdMapping[backupAccount.id] = newId
        }

        // Infer subcategory hierarchy from expenses if not present in backup
        // (backward compatibility with old backups that didn't save parentCategoryId)
        val categories = if (backupData.categories.all { it.parentCategoryId == null }) {
            val subcategoryToParent = mutableMapOf<Long, Long>()
            backupData.expenses.forEach { expense ->
                val subcatId = expense.subcategoryId
                val catId = expense.categoryId
                if (subcatId != null && catId != null && subcatId != catId) {
                    subcategoryToParent.putIfAbsent(subcatId, catId)
                }
            }
            backupData.categories.map { category ->
                val inferredParent = subcategoryToParent[category.id]
                if (inferredParent != null) category.copy(parentCategoryId = inferredParent)
                else category
            }
        } else {
            backupData.categories
        }

        // Restore root categories first (no parent), then subcategories
        val categoryIdMapping = mutableMapOf<Long, Long>()
        val rootCategories = categories.filter { it.parentCategoryId == null }
        val subcategories = categories.filter { it.parentCategoryId != null }

        rootCategories.forEach { backupCategory ->
            val category = backupCategory.toCategory()
            val newId = categoryRepository.insertCategory(category.copy(id = 0))
            categoryIdMapping[backupCategory.id] = newId
        }

        subcategories.forEach { backupCategory ->
            val category = backupCategory.toCategory()
            val mappedParentId = categoryIdMapping[backupCategory.parentCategoryId]
            val newId = categoryRepository.insertCategory(
                category.copy(id = 0, parentCategoryId = mappedParentId)
            )
            categoryIdMapping[backupCategory.id] = newId
        }

        // Restore expenses with mapped category and account IDs
        val expenses = backupData.expenses.map { backupExpense ->
            backupExpense.toExpense().copy(
                id = 0,
                categoryId = backupExpense.categoryId?.let { categoryIdMapping[it] },
                subcategoryId = backupExpense.subcategoryId?.let { categoryIdMapping[it] },
                accountId = backupExpense.accountId?.let { accountIdMapping[it] }
            )
        }
        expenseRepository.insertExpenses(expenses)
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}

@Serializable
data class BackupData(
    val version: Int,
    val timestamp: Long,
    val periodYear: Int? = null,
    val periodMonth: Int? = null,
    val expenses: List<BackupExpense>,
    val categories: List<BackupCategory>,
    val accounts: List<BackupAccount> = emptyList()
)

@Serializable
data class BackupExpense(
    val id: Long,
    val amount: Double,
    val note: String,
    val categoryId: Long?,
    val subcategoryId: Long? = null,
    val accountId: Long? = null,
    val type: String,
    val date: String,
    val createdAt: Long
)

@Serializable
data class BackupCategory(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val isDefault: Boolean,
    val parentCategoryId: Long? = null,
    val createdAt: Long,
    val displayOrder: Int = 0
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val type: String,
    val icon: String,
    val color: Long,
    val initialBalance: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: Long
)

private fun Expense.toBackupExpense() = BackupExpense(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    accountId = accountId,
    type = type.name,
    date = date.toString(),
    createdAt = createdAt
)

private fun BackupExpense.toExpense() = Expense(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    accountId = accountId,
    type = TransactionType.valueOf(type),
    date = LocalDate.parse(date),
    createdAt = createdAt
)

private fun Category.toBackupCategory() = BackupCategory(
    id = id,
    name = name,
    icon = icon,
    color = color.value.toLong(),
    isDefault = isDefault,
    parentCategoryId = parentCategoryId,
    createdAt = createdAt,
    displayOrder = displayOrder
)

private fun BackupCategory.toCategory() = Category(
    id = id,
    name = name,
    icon = icon,
    color = Color(color.toULong()),
    isDefault = isDefault,
    parentCategoryId = parentCategoryId,
    createdAt = createdAt,
    displayOrder = displayOrder
)

private fun Account.toBackupAccount() = BackupAccount(
    id = id,
    name = name,
    type = type.name,
    icon = icon,
    color = color.value.toLong(),
    initialBalance = initialBalance,
    isDefault = isDefault,
    createdAt = createdAt
)

private fun BackupAccount.toAccount() = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    icon = icon,
    color = Color(color.toULong()),
    initialBalance = initialBalance,
    isDefault = isDefault,
    createdAt = createdAt
)
