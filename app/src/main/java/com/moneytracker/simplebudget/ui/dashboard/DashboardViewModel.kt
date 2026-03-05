package com.moneytracker.simplebudget.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.repository.AccountRepository
import com.moneytracker.simplebudget.data.repository.CategoryRepository
import com.moneytracker.simplebudget.data.repository.ExpenseRepository
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.domain.model.ExpenseWithCategory
import com.moneytracker.simplebudget.domain.model.MonthlyStats
import com.moneytracker.simplebudget.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import com.moneytracker.simplebudget.domain.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class TransactionFilter(
    val transactionType: TransactionType? = null,
    val categoryId: Long? = null,
    val subcategoryId: Long? = null,
    val accountId: Long? = null,
    val searchQuery: String = ""
) {
    val isActive: Boolean
        get() = transactionType != null || categoryId != null || subcategoryId != null || accountId != null || searchQuery.isNotBlank()
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    val filteredTransactions: StateFlow<List<ExpenseWithCategory>> = combine(_uiState, _filter) { state, filter ->
        if (!filter.isActive) {
            state.recentTransactions
        } else {
            state.recentTransactions.filter { txn ->
                (filter.transactionType == null || txn.expense.type == filter.transactionType) &&
                (filter.categoryId == null || txn.expense.categoryId == filter.categoryId) &&
                (filter.subcategoryId == null || txn.expense.subcategoryId == filter.subcategoryId) &&
                (filter.accountId == null || txn.expense.accountId == filter.accountId) &&
                (filter.searchQuery.isBlank() || txn.expense.note.contains(filter.searchQuery, ignoreCase = true))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val currencySymbolAfter: StateFlow<Boolean> = preferencesManager.currencySymbolAfter
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isPremium: StateFlow<Boolean> = preferencesManager.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        initializeData()
        loadData()
    }

    private fun initializeData() {
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
            accountRepository.initializeDefaultAccount()
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            // Combine month selection, categories, and expenses reactively
            combine(
                categoryRepository.getAllCategories(),
                accountRepository.getAllAccounts(),
                _selectedMonth.flatMapLatest { month ->
                    expenseRepository.getExpensesByMonth(month.year, month.monthValue)
                }
            ) { categories, accounts, expenses ->
                Triple(categories, accounts, expenses)
            }.collect { (categories, accounts, expenses) ->
                updateUiState(categories, accounts, expenses)
            }
        }
    }

    private fun updateUiState(categories: List<Category>, accounts: List<com.moneytracker.simplebudget.domain.model.Account>, expenses: List<com.moneytracker.simplebudget.domain.model.Expense>) {
        val categoriesMap = categories.associateBy { it.id }
        val accountsMap = accounts.associateBy { it.id }

        // Calculate totals from the current expenses list (reactive)
        val totalIncome = expenses
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        val totalExpense = expenses
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        // Calculate category breakdown for expenses only
        val categoryTotals = expenses
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }

        val totalCategoryExpense = categoryTotals.values.sum()
        val breakdown = categoryTotals.map { (categoryId, amount) ->
            CategoryBreakdown(
                category = categoryId?.let { categoriesMap[it] },
                amount = amount,
                percentage = if (totalCategoryExpense > 0) (amount / totalCategoryExpense * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        val expensesWithCategory = expenses.map { expense ->
            ExpenseWithCategory(
                expense = expense,
                category = expense.categoryId?.let { categoriesMap[it] },
                subcategory = expense.subcategoryId?.let { categoriesMap[it] },
                account = expense.accountId?.let { accountsMap[it] },
                toAccount = expense.toAccountId?.let { accountsMap[it] }
            )
        }

        _uiState.value = DashboardUiState(
            isLoading = false,
            monthlyStats = MonthlyStats(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense,
                categoryBreakdown = breakdown
            ),
            recentTransactions = expensesWithCategory,
            categories = categories,
            accounts = accounts
        )
    }

    fun selectMonth(yearMonth: YearMonth) {
        _selectedMonth.value = yearMonth
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun setTransactionTypeFilter(type: TransactionType?) {
        _filter.value = _filter.value.copy(transactionType = type)
    }

    fun setCategoryFilter(categoryId: Long?) {
        _filter.value = _filter.value.copy(categoryId = categoryId, subcategoryId = null)
    }

    fun setSubcategoryFilter(subcategoryId: Long?) {
        _filter.value = _filter.value.copy(subcategoryId = subcategoryId)
    }

    fun setAccountFilter(accountId: Long?) {
        _filter.value = _filter.value.copy(accountId = accountId)
    }

    fun setSearchQuery(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun clearFilters() {
        _filter.value = TransactionFilter()
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseById(expenseId)
            // No need to manually refresh - the Flow will automatically emit new data
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val monthlyStats: MonthlyStats = MonthlyStats(0.0, 0.0, 0.0, emptyList()),
    val recentTransactions: List<ExpenseWithCategory> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList()
)
