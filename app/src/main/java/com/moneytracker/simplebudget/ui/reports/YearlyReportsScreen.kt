package com.moneytracker.simplebudget.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.ui.components.CollapsibleSummaryCard
import com.moneytracker.simplebudget.ui.components.CurrencyAmountText
import com.moneytracker.simplebudget.ui.components.MonthlyBarChart
import com.moneytracker.simplebudget.ui.components.ScrollToTopButton
import com.moneytracker.simplebudget.ui.components.rememberCollapseProgress
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearlyReportsScreen(
    currency: String,
    symbolAfter: Boolean = true,
    viewModel: YearlyReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    var showYearPicker by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val collapseProgress = rememberCollapseProgress(listState)
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
                        dragOffset.animateTo(
                            if (goingBack) screenWidthPx else -screenWidthPx,
                            tween(150)
                        )
                        viewModel.selectYear(year)
                        dragOffset.snapTo(
                            if (goingBack) -screenWidthPx else screenWidthPx
                        )
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    // Scroll to top when year changes
    LaunchedEffect(selectedYear) {
        listState.scrollToItem(0)
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yearly Reports") }
            )
        },
        floatingActionButton = {
            ScrollToTopButton(
                listState = listState,
                onClick = {
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Pinned year selector
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                YearSelector(
                    selectedYear = selectedYear,
                    onPreviousYear = animateToPrevious,
                    onNextYear = animateToNext,
                    onYearClick = { showYearPicker = true }
                )
            }

            // Pinned collapsible hero — swipes with content
            CollapsibleSummaryCard(
                income = uiState.totalIncome,
                expense = uiState.totalExpense,
                balance = uiState.balance,
                currency = currency,
                collapseProgress = collapseProgress,
                symbolAfter = symbolAfter,
                balanceLabel = "Yearly Balance",
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
                                    val currentOffset = dragOffset.value
                                    if (currentOffset > swipeThreshold) {
                                        dragOffset.animateTo(screenWidthPx, tween(150))
                                        viewModel.previousYear()
                                        dragOffset.snapTo(-screenWidthPx)
                                        dragOffset.animateTo(0f, tween(200))
                                    } else if (currentOffset < -swipeThreshold) {
                                        dragOffset.animateTo(-screenWidthPx, tween(150))
                                        viewModel.nextYear()
                                        dragOffset.snapTo(screenWidthPx)
                                        dragOffset.animateTo(0f, tween(200))
                                    } else {
                                        dragOffset.animateTo(0f, tween(200))
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch { dragOffset.animateTo(0f, tween(200)) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                coroutineScope.launch {
                                    dragOffset.snapTo(dragOffset.value + dragAmount)
                                }
                            }
                        )
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Content with interactive drag offset
                item {
                    Column(
                        modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Bar Chart - Monthly Overview
                        if (uiState.monthlyData.isNotEmpty()) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Monthly Overview",
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

                        // Expense Pie Chart
                        if (uiState.expenseBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = "Yearly Expenses by Category",
                                breakdown = uiState.expenseBreakdown,
                                currency = currency,
                                color = ExpenseRed,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Income Pie Chart
                        if (uiState.incomeBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = "Yearly Income by Category",
                                breakdown = uiState.incomeBreakdown,
                                currency = currency,
                                color = IncomeGreen,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Monthly Breakdown List
                        if (uiState.monthlyData.any { it.income > 0 || it.expense > 0 }) {
                            MonthlyBreakdownCard(
                                monthlyData = uiState.monthlyData,
                                currency = currency,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Empty state
                        if (uiState.monthlyData.all { it.income == 0.0 && it.expense == 0.0 } && !uiState.isLoading) {
                            EmptyYearlyReportsState()
                            Spacer(modifier = Modifier.height(300.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YearSelector(
    selectedYear: Int,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onYearClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousYear) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
        }

        Text(
            text = selectedYear.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onYearClick)
        )

        IconButton(
            onClick = onNextYear,
            enabled = true
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
        }
    }
}

@Composable
fun YearPickerDialog(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = Year.now().value
    var pickedYear by remember { mutableIntStateOf(selectedYear) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                val years = (currentYear - 10)..(currentYear + 5)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    years.chunked(4).forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowYears.forEach { year ->
                                val isSelected = year == pickedYear
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { pickedYear = year },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = year.toString(),
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        textAlign = TextAlign.Center,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                            repeat(4 - rowYears.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onYearSelected(pickedYear) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MonthlyBreakdownCard(
    monthlyData: List<MonthData>,
    currency: String,
    symbolAfter: Boolean = true
) {
    val maxAmount = monthlyData.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Monthly Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            monthlyData.forEachIndexed { index, data ->
                if (data.income > 0 || data.expense > 0) {
                    val monthName = Month.of(index + 1).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val balance = data.income - data.expense
                    val incomeFraction = if (maxAmount > 0) (data.income / maxAmount).toFloat() else 0f
                    val expenseFraction = if (maxAmount > 0) (data.expense / maxAmount).toFloat() else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            CurrencyAmountText(
                                amount = balance,
                                currencyCode = currency,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (balance >= 0) IncomeGreen else ExpenseRed,
                                symbolAfter = symbolAfter
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(incomeFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(IncomeGreen)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.width(110.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                CurrencyAmountText(
                                    amount = data.income,
                                    currencyCode = currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IncomeGreen,
                                    maxLines = 1,
                                    symbolAfter = symbolAfter
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(expenseFraction.coerceAtLeast(0.02f))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ExpenseRed)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(
                                modifier = Modifier.width(110.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                CurrencyAmountText(
                                    amount = data.expense,
                                    currencyCode = currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ExpenseRed,
                                    maxLines = 1,
                                    symbolAfter = symbolAfter
                                )
                            }
                        }
                    }

                    if (index < monthlyData.lastIndex && (monthlyData[index + 1].income > 0 || monthlyData[index + 1].expense > 0)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyYearlyReportsState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No data for this year",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add transactions to see yearly reports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
