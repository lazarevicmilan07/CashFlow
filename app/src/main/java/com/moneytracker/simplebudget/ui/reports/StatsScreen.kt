package com.moneytracker.simplebudget.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.ui.components.CollapsibleSummaryCard
import com.moneytracker.simplebudget.ui.components.MonthSelector
import com.moneytracker.simplebudget.ui.components.MonthYearPickerDialog
import com.moneytracker.simplebudget.ui.components.MonthlyBarChart
import com.moneytracker.simplebudget.ui.components.ScrollToTopButton
import com.moneytracker.simplebudget.ui.components.TogglePill
import com.moneytracker.simplebudget.ui.components.rememberCollapseProgress
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    currency: String,
    symbolAfter: Boolean = true,
    monthlyViewModel: MonthlyReportsViewModel = hiltViewModel(),
    yearlyViewModel: YearlyReportsViewModel = hiltViewModel()
) {
    var isMonthly by remember { mutableStateOf(true) }
    val monthlyListState = rememberLazyListState()
    val yearlyListState = rememberLazyListState()
    val activeListState = if (isMonthly) monthlyListState else yearlyListState
    val collapseProgress = rememberCollapseProgress(activeListState)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                actions = {
                    TogglePill(leftLabel = stringResource(R.string.reports_tab_monthly), rightLabel = stringResource(R.string.reports_tab_yearly), leftSelected = isMonthly, onSelect = { isMonthly = it })
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                listState = activeListState,
                onClick = { coroutineScope.launch { activeListState.animateScrollToItem(0) } },
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }
    ) { paddingValues ->
        if (isMonthly) {
            MonthlyStatsContent(
                currency = currency,
                symbolAfter = symbolAfter,
                viewModel = monthlyViewModel,
                collapseProgress = collapseProgress,
                paddingValues = paddingValues,
                listState = monthlyListState
            )
        } else {
            YearlyStatsContent(
                currency = currency,
                symbolAfter = symbolAfter,
                viewModel = yearlyViewModel,
                collapseProgress = collapseProgress,
                paddingValues = paddingValues,
                listState = yearlyListState
            )
        }
    }
}

@Composable
private fun MonthlyStatsContent(
    currency: String,
    symbolAfter: Boolean,
    viewModel: MonthlyReportsViewModel,
    collapseProgress: Float,
    paddingValues: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    if (showMonthPicker) {
        MonthYearPickerDialog(
            selectedMonth = selectedMonth,
            onMonthSelected = { yearMonth ->
                if (yearMonth != selectedMonth) {
                    val goingBack = yearMonth < selectedMonth
                    coroutineScope.launch {
                        dragOffset.animateTo(if (goingBack) screenWidthPx else -screenWidthPx, tween(150))
                        viewModel.selectMonth(yearMonth)
                        dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    LaunchedEffect(selectedMonth) { listState.scrollToItem(0) }

    val animateToPrevious: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousMonth()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }
    val animateToNext: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextMonth()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            MonthSelector(
                selectedMonth = selectedMonth,
                onPreviousMonth = animateToPrevious,
                onNextMonth = animateToNext,
                onMonthClick = { showMonthPicker = true }
            )
        }

        CollapsibleSummaryCard(
            income = uiState.totalIncome,
            expense = uiState.totalExpense,
            balance = uiState.balance,
            currency = currency,
            collapseProgress = collapseProgress,
            modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
            symbolAfter = symbolAfter
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedMonth) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val offset = dragOffset.value
                                if (offset > swipeThreshold) {
                                    dragOffset.animateTo(screenWidthPx, tween(150))
                                    viewModel.previousMonth()
                                    dragOffset.snapTo(-screenWidthPx)
                                    dragOffset.animateTo(0f, tween(200))
                                } else if (offset < -swipeThreshold) {
                                    dragOffset.animateTo(-screenWidthPx, tween(150))
                                    viewModel.nextMonth()
                                    dragOffset.snapTo(screenWidthPx)
                                    dragOffset.animateTo(0f, tween(200))
                                } else {
                                    dragOffset.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onDragCancel = { coroutineScope.launch { dragOffset.animateTo(0f, tween(200)) } },
                        onHorizontalDrag = { _, amount -> coroutineScope.launch { dragOffset.snapTo(dragOffset.value + amount) } }
                    )
                },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.expenseBreakdown.isNotEmpty()) {
                        BreakdownCard(
                            title = stringResource(R.string.reports_expenses_by_category),
                            breakdown = uiState.expenseBreakdown,
                            currency = currency,
                            color = ExpenseRed,
                            symbolAfter = symbolAfter
                        )
                    }
                    if (uiState.incomeBreakdown.isNotEmpty()) {
                        BreakdownCard(
                            title = stringResource(R.string.reports_income_by_category),
                            breakdown = uiState.incomeBreakdown,
                            currency = currency,
                            color = IncomeGreen,
                            symbolAfter = symbolAfter
                        )
                    }
                    if (uiState.expenseBreakdown.isEmpty() && uiState.incomeBreakdown.isEmpty() && !uiState.isLoading) {
                        EmptyReportsState()
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyStatsContent(
    currency: String,
    symbolAfter: Boolean,
    viewModel: YearlyReportsViewModel,
    collapseProgress: Float,
    paddingValues: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    var showYearPicker by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    if (showYearPicker) {
        YearPickerDialog(
            selectedYear = selectedYear,
            onYearSelected = { year ->
                if (year != selectedYear) {
                    val goingBack = year < selectedYear
                    coroutineScope.launch {
                        dragOffset.animateTo(if (goingBack) screenWidthPx else -screenWidthPx, tween(150))
                        viewModel.selectYear(year)
                        dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    LaunchedEffect(selectedYear) { listState.scrollToItem(0) }

    val animateToPrevious: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousYear()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }
    val animateToNext: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextYear()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            YearSelector(
                selectedYear = selectedYear,
                onPreviousYear = animateToPrevious,
                onNextYear = animateToNext,
                onYearClick = { showYearPicker = true }
            )
        }

        CollapsibleSummaryCard(
            income = uiState.totalIncome,
            expense = uiState.totalExpense,
            balance = uiState.balance,
            currency = currency,
            collapseProgress = collapseProgress,
            symbolAfter = symbolAfter,
            balanceLabel = stringResource(R.string.reports_yearly_balance),
            modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedYear) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val offset = dragOffset.value
                                if (offset > swipeThreshold) {
                                    dragOffset.animateTo(screenWidthPx, tween(150))
                                    viewModel.previousYear()
                                    dragOffset.snapTo(-screenWidthPx)
                                    dragOffset.animateTo(0f, tween(200))
                                } else if (offset < -swipeThreshold) {
                                    dragOffset.animateTo(-screenWidthPx, tween(150))
                                    viewModel.nextYear()
                                    dragOffset.snapTo(screenWidthPx)
                                    dragOffset.animateTo(0f, tween(200))
                                } else {
                                    dragOffset.animateTo(0f, tween(200))
                                }
                            }
                        },
                        onDragCancel = { coroutineScope.launch { dragOffset.animateTo(0f, tween(200)) } },
                        onHorizontalDrag = { _, amount -> coroutineScope.launch { dragOffset.snapTo(dragOffset.value + amount) } }
                    )
                },
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (uiState.monthlyData.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = stringResource(R.string.reports_monthly_overview),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                MonthlyBarChart(
                                    monthlyData = uiState.monthlyData,
                                    currency = currency,
                                    symbolAfter = symbolAfter,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    if (uiState.expenseBreakdown.isNotEmpty()) {
                        BreakdownCard(
                            title = stringResource(R.string.reports_yearly_expenses_by_category),
                            breakdown = uiState.expenseBreakdown,
                            currency = currency,
                            color = ExpenseRed,
                            symbolAfter = symbolAfter
                        )
                    }
                    if (uiState.incomeBreakdown.isNotEmpty()) {
                        BreakdownCard(
                            title = stringResource(R.string.reports_yearly_income_by_category),
                            breakdown = uiState.incomeBreakdown,
                            currency = currency,
                            color = IncomeGreen,
                            symbolAfter = symbolAfter
                        )
                    }
                    if (uiState.monthlyData.any { it.income > 0 || it.expense > 0 }) {
                        MonthlyBreakdownCard(
                            monthlyData = uiState.monthlyData,
                            currency = currency,
                            symbolAfter = symbolAfter
                        )
                    }
                    if (uiState.monthlyData.all { it.income == 0.0 && it.expense == 0.0 } && !uiState.isLoading) {
                        EmptyYearlyReportsState()
                        Spacer(modifier = Modifier.height(300.dp))
                    }
                }
            }
        }
    }
}

