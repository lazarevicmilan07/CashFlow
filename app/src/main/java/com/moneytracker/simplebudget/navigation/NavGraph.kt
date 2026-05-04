package com.moneytracker.simplebudget.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.moneytracker.simplebudget.ui.accounts.AccountsScreen
import com.moneytracker.simplebudget.ui.budget.BudgetFormScreen
import com.moneytracker.simplebudget.ui.budget.BudgetScreen
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.ui.categories.CategoriesScreen
import com.moneytracker.simplebudget.ui.dashboard.DashboardScreen
import com.moneytracker.simplebudget.ui.dashboard.DashboardViewModel
import com.moneytracker.simplebudget.ui.premium.PremiumScreen
import com.moneytracker.simplebudget.ui.reports.StatsScreen
import com.moneytracker.simplebudget.ui.settings.SettingsScreen
import com.moneytracker.simplebudget.ui.transaction.TransactionDetailScreen
import com.moneytracker.simplebudget.ui.transaction.TransactionScreen

// Nav bar position index for directional animations
private fun getNavBarIndex(route: String?): Int {
    return when {
        route == null -> -1
        route == Screen.Dashboard.route -> 0
        route == Screen.Budget.route -> 1
        route == Screen.Stats.route -> 2
        route == Screen.Accounts.route -> 3
        route == Screen.Categories.route -> 4
        route == Screen.Settings.route -> 5
        else -> -1 // Non-nav-bar screens default to -1
    }
}

private fun isNavBarRoute(route: String?): Boolean = getNavBarIndex(route) >= 0

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Budget : Screen("budget")
    data object Stats : Screen("stats")
    data object AddTransaction : Screen("add_transaction")
    data object EditTransaction : Screen("edit_transaction/{expenseId}") {
        fun createRoute(expenseId: Long) = "edit_transaction/$expenseId"
    }
    data object TransactionDetail : Screen("transaction_detail/{expenseId}") {
        fun createRoute(expenseId: Long) = "transaction_detail/$expenseId"
    }
    data object Categories : Screen("categories")
    data object Accounts : Screen("accounts")
    data object Settings : Screen("settings")
    data object Premium : Screen("premium")
    data object CopyTransaction : Screen("copy_transaction/{expenseId}/{useToday}") {
        fun createRoute(expenseId: Long, useToday: Boolean) = "copy_transaction/$expenseId/$useToday"
    }
    data object AccountsFromTransaction : Screen("accounts_from_transaction")
    data object CategoriesFromTransaction : Screen("categories_from_transaction")
    data object BudgetForm : Screen("budget_form?budgetId={budgetId}&year={year}&month={month}&period={period}") {
        fun createRoute(budgetId: Long = 0L, year: Int, month: Int, period: BudgetPeriod) =
            "budget_form?budgetId=$budgetId&year=$year&month=$month&period=${period.name}"
    }

}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier,
        enterTransition = {
            val fromIndex = getNavBarIndex(initialState.destination.route)
            val toIndex = getNavBarIndex(targetState.destination.route)
            // If both are nav bar routes, use directional animation
            if (fromIndex >= 0 && toIndex >= 0) {
                if (toIndex > fromIndex) {
                    // Going right: slide in from right
                    slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))
                } else {
                    // Going left: slide in from left
                    slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300))
                }
            } else {
                // Default: slide in from right
                slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))
            }
        },
        exitTransition = {
            val fromIndex = getNavBarIndex(initialState.destination.route)
            val toIndex = getNavBarIndex(targetState.destination.route)
            if (fromIndex >= 0 && toIndex >= 0) {
                if (toIndex > fromIndex) {
                    // Going right: current screen slides out to left
                    slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
                } else {
                    // Going left: current screen slides out to right
                    slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300))
                }
            } else {
                slideOutHorizontally(tween(300)) { -it / 3 } + fadeOut(tween(300))
            }
        },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 3 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300)) }
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddTransaction.route)
                },
                onViewTransaction = { expenseId ->
                    navController.navigate(Screen.EditTransaction.createRoute(expenseId))
                }
            )
        }

        composable(Screen.AddTransaction.route) {
            TransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.EditTransaction.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) {
            TransactionScreen(
                onNavigateBack = { navController.popBackStack() },
                onCopyTransaction = { expenseId, useToday ->
                    navController.navigate(Screen.CopyTransaction.createRoute(expenseId, useToday))
                },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.CopyTransaction.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType },
                navArgument("useToday") { type = NavType.BoolType }
            )
        ) {
            TransactionScreen(
                onNavigateBack = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                },
                onNavigateToAccounts = { navController.navigate(Screen.AccountsFromTransaction.route) },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(
                navArgument("expenseId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: -1L
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            val symbolAfter by dashboardViewModel.currencySymbolAfter.collectAsState()
            TransactionDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onEditTransaction = {
                    navController.navigate(Screen.EditTransaction.createRoute(expenseId))
                },
                currency = currency,
                symbolAfter = symbolAfter
            )
        }

        composable(Screen.Categories.route) {
            CategoriesScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Accounts.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            val symbolAfter by dashboardViewModel.currencySymbolAfter.collectAsState()
            AccountsScreen(
                currency = currency,
                symbolAfter = symbolAfter
            )
        }

        composable(Screen.AccountsFromTransaction.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            val symbolAfter by dashboardViewModel.currencySymbolAfter.collectAsState()
            AccountsScreen(
                onNavigateBack = { navController.popBackStack() },
                currency = currency,
                symbolAfter = symbolAfter
            )
        }

        composable(Screen.CategoriesFromTransaction.route) {
            CategoriesScreen(
                onNavigateBack = { navController.popBackStack() },
                onShowPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) }
            )
        }

        composable(Screen.Premium.route) {
            PremiumScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Stats.route) {
            val dashboardViewModel: DashboardViewModel = hiltViewModel()
            val currency by dashboardViewModel.currency.collectAsState()
            val symbolAfter by dashboardViewModel.currencySymbolAfter.collectAsState()
            StatsScreen(currency = currency, symbolAfter = symbolAfter)
        }

        composable(Screen.Budget.route) {
            BudgetScreen(
                onShowPremium = { navController.navigate(Screen.Premium.route) },
                onNavigateToForm = { budgetId, year, month, period ->
                    navController.navigate(Screen.BudgetForm.createRoute(budgetId, year, month, period))
                }
            )
        }

        composable(
            route = Screen.BudgetForm.route,
            arguments = listOf(
                navArgument("budgetId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("year") { type = NavType.IntType; defaultValue = java.time.YearMonth.now().year },
                navArgument("month") { type = NavType.IntType; defaultValue = java.time.YearMonth.now().monthValue },
                navArgument("period") { type = NavType.StringType; defaultValue = BudgetPeriod.MONTHLY.name }
            )
        ) { backStackEntry ->
            val year = backStackEntry.arguments?.getInt("year") ?: java.time.YearMonth.now().year
            val month = backStackEntry.arguments?.getInt("month") ?: java.time.YearMonth.now().monthValue
            val period = BudgetPeriod.valueOf(backStackEntry.arguments?.getString("period") ?: BudgetPeriod.MONTHLY.name)
            BudgetFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Screen.CategoriesFromTransaction.route) }
            )
        }
    }
}
