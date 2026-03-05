package com.moneytracker.simplebudget.ui.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.ui.components.CollapsibleSummaryCard
import com.moneytracker.simplebudget.ui.components.CurrencyAmountText
import com.moneytracker.simplebudget.ui.components.MonthSelector
import com.moneytracker.simplebudget.ui.components.MonthYearPickerDialog
import com.moneytracker.simplebudget.ui.components.ScrollToTopButton
import com.moneytracker.simplebudget.ui.components.rememberCollapseProgress
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    currency: String,
    symbolAfter: Boolean = true,
    viewModel: MonthlyReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val collapseProgress = rememberCollapseProgress(listState)
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
                        dragOffset.animateTo(
                            if (goingBack) screenWidthPx else -screenWidthPx,
                            tween(150)
                        )
                        viewModel.selectMonth(yearMonth)
                        dragOffset.snapTo(
                            if (goingBack) -screenWidthPx else screenWidthPx
                        )
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Scroll to top when month changes
    LaunchedEffect(selectedMonth) {
        listState.scrollToItem(0)
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Reports") }
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
            // Pinned month selector
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onPreviousMonth = animateToPrevious,
                    onNextMonth = animateToNext,
                    onMonthClick = { showMonthPicker = true }
                )
            }

            // Pinned collapsible hero — swipes with content
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
                                    val currentOffset = dragOffset.value
                                    if (currentOffset > swipeThreshold) {
                                        dragOffset.animateTo(screenWidthPx, tween(150))
                                        viewModel.previousMonth()
                                        dragOffset.snapTo(-screenWidthPx)
                                        dragOffset.animateTo(0f, tween(200))
                                    } else if (currentOffset < -swipeThreshold) {
                                        dragOffset.animateTo(-screenWidthPx, tween(150))
                                        viewModel.nextMonth()
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
                        // Expense Pie Chart
                        if (uiState.expenseBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = "Expenses by Category",
                                breakdown = uiState.expenseBreakdown,
                                currency = currency,
                                color = ExpenseRed,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Income Pie Chart
                        if (uiState.incomeBreakdown.isNotEmpty()) {
                            BreakdownCard(
                                title = "Income by Category",
                                breakdown = uiState.incomeBreakdown,
                                currency = currency,
                                color = IncomeGreen,
                                symbolAfter = symbolAfter
                            )
                        }

                        // Empty state
                        if (uiState.expenseBreakdown.isEmpty() && uiState.incomeBreakdown.isEmpty() && !uiState.isLoading) {
                            EmptyReportsState()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownCard(
    title: String,
    breakdown: List<CategoryBreakdown>,
    currency: String,
    color: Color,
    symbolAfter: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pie Chart
            PieChart(
                breakdown = breakdown,
                currency = currency,
                accentColor = color,
                symbolAfter = symbolAfter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            breakdown.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.category?.color ?: Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.category?.name ?: "Uncategorized",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${item.percentage.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    CurrencyAmountText(
                        amount = item.amount,
                        currencyCode = currency,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        symbolAfter = symbolAfter
                    )
                }
            }
        }
    }
}

@Composable
fun PieChart(
    breakdown: List<CategoryBreakdown>,
    currency: String,
    accentColor: Color = Color.Unspecified,
    symbolAfter: Boolean = true,
    modifier: Modifier = Modifier
) {
    val total = breakdown.sumOf { it.amount }.toFloat()
    var selectedIndex by remember { mutableIntStateOf(-1) }
    val gapDegrees = if (breakdown.size > 1) 2.5f else 0f

    val sliceAngles = remember(breakdown, total) {
        var cumulative = -90f
        breakdown.map { item ->
            val sweep = if (total > 0f) (item.amount.toFloat() / total * 360f) else 0f
            val start = cumulative
            cumulative += sweep
            start to sweep
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = modifier.pointerInput(breakdown) {
                    detectTapGestures { offset ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                        val outerR = minOf(size.width, size.height) / 2f * 0.85f
                        val innerR = outerR * 0.35f
                        if (dist in innerR..outerR) {
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < -90f) angle += 360f
                            val tapped = sliceAngles.indexOfFirst { (start, sweep) ->
                                angle >= start && angle < start + sweep
                            }
                            selectedIndex = if (tapped == selectedIndex) -1 else tapped
                        } else {
                            selectedIndex = -1
                        }
                    }
                }
            ) {
                val canvasSize = size.minDimension
                val outerRadius = canvasSize / 2f * 0.85f
                val strokeWidth = outerRadius * 0.6f
                val arcRadius = outerRadius - strokeWidth / 2f
                val centerX = size.width / 2f
                val centerY = size.height / 2f

                sliceAngles.forEachIndexed { index, (start, sweep) ->
                    val sliceColor = breakdown[index].category?.color ?: Color.Gray
                    val isSelected = index == selectedIndex
                    val drawStroke = if (isSelected) strokeWidth * 1.15f else strokeWidth
                    val adjustedSweep = (sweep - gapDegrees).coerceAtLeast(0.5f)
                    val adjustedStart = start + gapDegrees / 2f
                    drawArc(
                        color = if (selectedIndex != -1 && !isSelected) sliceColor.copy(alpha = 0.4f) else sliceColor,
                        startAngle = adjustedStart,
                        sweepAngle = adjustedSweep,
                        useCenter = false,
                        topLeft = Offset(centerX - arcRadius, centerY - arcRadius),
                        size = Size(arcRadius * 2f, arcRadius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = drawStroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Butt
                        )
                    )
                }
            }
        }

        if (selectedIndex in breakdown.indices) {
            val item = breakdown[selectedIndex]
            val tooltipColor = if (accentColor != Color.Unspecified) accentColor else MaterialTheme.colorScheme.onSurface
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${item.category?.name ?: "Uncategorized"}: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tooltipColor
                )
                CurrencyAmountText(
                    amount = item.amount,
                    currencyCode = currency,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tooltipColor,
                    symbolAfter = symbolAfter
                )
                Text(
                    text = " (${item.percentage.toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tooltipColor
                )
            }
        }
    }
}

@Composable
fun EmptyReportsState() {
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
                text = "No data for this month",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add transactions to see reports",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
