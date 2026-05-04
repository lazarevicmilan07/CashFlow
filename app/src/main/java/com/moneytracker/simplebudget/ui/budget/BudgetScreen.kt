package com.moneytracker.simplebudget.ui.budget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.domain.model.BudgetWithProgress
import com.moneytracker.simplebudget.ui.components.CategoryIcon
import com.moneytracker.simplebudget.ui.components.MonthSelector
import com.moneytracker.simplebudget.ui.components.MonthYearPickerDialog
import com.moneytracker.simplebudget.ui.components.TogglePill
import com.moneytracker.simplebudget.ui.reports.YearPickerDialog
import com.moneytracker.simplebudget.ui.reports.YearSelector
import com.moneytracker.simplebudget.ui.components.formatCurrency
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.math.roundToInt

private val BudgetAmber = Color(0xFFFFA726)
private val BudgetAmberLight = Color(0xFFFFF3E0)
private val BudgetGreenLight = Color(0xFFE8F5E9)
private val BudgetRedLight = Color(0xFFFFEBEE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onShowPremium: () -> Unit,
    onNavigateToForm: (Long, Int, Int, BudgetPeriod) -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedPeriodFilter by viewModel.selectedPeriodFilter.collectAsState()
    val budgets by viewModel.budgets.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val symbolAfter by viewModel.currencySymbolAfter.collectAsState()

    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showPremiumLimitDialog by remember { mutableStateOf(false) }

    val swipeThreshold = 100f
    val dragOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(selectedMonth) { lazyListState.scrollToItem(0) }

    val animateToPrevious: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            if (selectedPeriodFilter == BudgetPeriod.MONTHLY) viewModel.selectPreviousMonth()
            else viewModel.selectPreviousYear()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }
    val animateToNext: () -> Unit = {
        coroutineScope.launch {
            dragOffset.animateTo(-screenWidthPx, tween(150))
            if (selectedPeriodFilter == BudgetPeriod.MONTHLY) viewModel.selectNextMonth()
            else viewModel.selectNextYear()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.budget_title)) },
                actions = {
                    TogglePill(
                        leftLabel = stringResource(R.string.budget_period_monthly),
                        rightLabel = stringResource(R.string.budget_period_yearly),
                        leftSelected = selectedPeriodFilter == BudgetPeriod.MONTHLY,
                        onSelect = { isLeft ->
                            viewModel.selectPeriodFilter(if (isLeft) BudgetPeriod.MONTHLY else BudgetPeriod.YEARLY)
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FloatingActionButton(
                    onClick = {
                        viewModel.canAddBudget(
                            onBlocked = { showPremiumLimitDialog = true },
                            onAllowed = {
                                onNavigateToForm(
                                    0L,
                                    selectedMonth.year,
                                    selectedMonth.monthValue,
                                    selectedPeriodFilter
                                )
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.budget_add))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (selectedPeriodFilter == BudgetPeriod.MONTHLY) {
                    MonthSelector(
                        selectedMonth = selectedMonth,
                        onPreviousMonth = animateToPrevious,
                        onNextMonth = animateToNext,
                        onMonthClick = { showMonthPicker = true }
                    )
                } else {
                    YearSelector(
                        selectedYear = selectedMonth.year,
                        onPreviousYear = animateToPrevious,
                        onNextYear = animateToNext,
                        onYearClick = { showYearPicker = true }
                    )
                }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedMonth, selectedPeriodFilter) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    val offset = dragOffset.value
                                    when {
                                        offset > swipeThreshold -> {
                                            dragOffset.animateTo(screenWidthPx, tween(150))
                                            if (selectedPeriodFilter == BudgetPeriod.MONTHLY) viewModel.selectPreviousMonth()
                                            else viewModel.selectPreviousYear()
                                            dragOffset.snapTo(-screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        offset < -swipeThreshold -> {
                                            dragOffset.animateTo(-screenWidthPx, tween(150))
                                            if (selectedPeriodFilter == BudgetPeriod.MONTHLY) viewModel.selectNextMonth()
                                            else viewModel.selectNextYear()
                                            dragOffset.snapTo(screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        else -> dragOffset.animateTo(0f, tween(200))
                                    }
                                }
                            },
                            onDragCancel = { coroutineScope.launch { dragOffset.animateTo(0f, tween(200)) } },
                            onHorizontalDrag = { _, amount ->
                                coroutineScope.launch { dragOffset.snapTo(dragOffset.value + amount) }
                            }
                        )
                    },
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (budgets.isEmpty()) {
                            Spacer(modifier = Modifier.height(80.dp))
                            EmptyBudgetState()
                        } else {
                            val totalBudget = budgets.sumOf { it.budget.amount }
                            val totalSpent = budgets.sumOf { it.spent }
                            BudgetSummaryCard(
                                totalBudget = totalBudget,
                                totalSpent = totalSpent,
                                currency = currency,
                                symbolAfter = symbolAfter
                            )
                            budgets.forEach { budgetProgress ->
                                BudgetCard(
                                    budgetWithProgress = budgetProgress,
                                    currency = currency,
                                    symbolAfter = symbolAfter,
                                    onClick = {
                                        onNavigateToForm(
                                            budgetProgress.budget.id,
                                            budgetProgress.budget.year,
                                            budgetProgress.budget.month ?: selectedMonth.monthValue,
                                            budgetProgress.budget.period
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
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

    if (showYearPicker) {
        YearPickerDialog(
            selectedYear = selectedMonth.year,
            onYearSelected = { year ->
                val newYearMonth = YearMonth.of(year, selectedMonth.monthValue)
                if (newYearMonth != selectedMonth) {
                    val goingBack = newYearMonth < selectedMonth
                    coroutineScope.launch {
                        dragOffset.animateTo(if (goingBack) screenWidthPx else -screenWidthPx, tween(150))
                        viewModel.selectMonth(newYearMonth)
                        dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                        dragOffset.animateTo(0f, tween(200))
                    }
                }
                showYearPicker = false
            },
            onDismiss = { showYearPicker = false }
        )
    }

    if (showPremiumLimitDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumLimitDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.budget_limit_reached_title)) },
            text = { Text(stringResource(R.string.budget_limit_reached_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showPremiumLimitDialog = false
                    onShowPremium()
                }) { Text("Upgrade") }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumLimitDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    totalBudget: Double,
    totalSpent: Double,
    currency: String,
    symbolAfter: Boolean
) {
    val percentage = if (totalBudget > 0) (totalSpent / totalBudget).toFloat().coerceIn(0f, 1f) else 0f
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(800),
        label = "summary_ring"
    )
    val ringColor = budgetColor(percentage)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(ringColor.copy(alpha = 0.08f), Color.Transparent)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(88.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.size(88.dp)) {
                            val strokeWidth = 10.dp.toPx()
                            val inset = strokeWidth / 2f
                            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedPercentage,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "${(percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ringColor
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.budget_total_budget),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(totalSpent, currency, symbolAfter),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stringResource(R.string.budget_of)} ${formatCurrency(totalBudget, currency, symbolAfter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val remaining = totalBudget - totalSpent
                        val isOver = remaining < 0
                        Text(
                            text = if (isOver) "${formatCurrency(-remaining, currency, symbolAfter)} ${stringResource(R.string.budget_hint_over)}"
                                   else "${formatCurrency(remaining, currency, symbolAfter)} ${stringResource(R.string.budget_remaining)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver) ExpenseRed else IncomeGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                BudgetProgressBar(
                    progress = percentage,
                    color = ringColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                )
            }
        }
    }
}

@Composable
fun BudgetCard(
    budgetWithProgress: BudgetWithProgress,
    currency: String,
    symbolAfter: Boolean,
    onClick: () -> Unit
) {
    val percentage = budgetWithProgress.percentage.coerceIn(0f, Float.MAX_VALUE)
    val displayPercentage = percentage.coerceIn(0f, 1f)
    val progressColor = budgetColor(percentage)
    val isOverBudget = budgetWithProgress.remaining < 0
    val percentInt = (displayPercentage * 100).toInt()

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardTint = when {
        isOverBudget || percentage >= 0.9f -> if (isDark) Color(0xFF2A1515) else BudgetRedLight
        percentage >= 0.65f -> if (isDark) Color(0xFF2A1F0D) else BudgetAmberLight
        else -> MaterialTheme.colorScheme.surface
    }

    val category = budgetWithProgress.category
    val subcategory = budgetWithProgress.subcategory
    val displayName = when {
        subcategory != null -> "${category?.name ?: ""} › ${subcategory.name}"
        category != null -> category.name
        else -> stringResource(R.string.budget_overall)
    }
    val iconCategory = subcategory ?: category

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardTint,
        onClick = onClick,
        tonalElevation = if (cardTint == MaterialTheme.colorScheme.surface) 1.dp else 0.dp,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(progressColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (iconCategory != null) {
                        CategoryIcon(
                            icon = iconCategory.icon,
                            color = iconCategory.color,
                            size = 44.dp,
                            iconSize = 24.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(progressColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percentInt%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = progressColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BudgetProgressBar(
                    progress = displayPercentage,
                    color = progressColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = formatCurrency(budgetWithProgress.spent, currency, symbolAfter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/ ${formatCurrency(budgetWithProgress.budget.amount, currency, symbolAfter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isOverBudget)
                        "${formatCurrency(-budgetWithProgress.remaining, currency, symbolAfter)} ${stringResource(R.string.budget_hint_over)}"
                    else
                        "${formatCurrency(budgetWithProgress.remaining, currency, symbolAfter)} ${stringResource(R.string.budget_remaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isOverBudget) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isOverBudget) ExpenseRed else IncomeGreen
                )
            }
        }
    }
}

@Composable
private fun EmptyBudgetState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "💰",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.budget_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.budget_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackAlpha: Float = 0.18f
) {
    val animatedProgress = remember(progress) { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Box(modifier = modifier.clip(RoundedCornerShape(50))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val fillW = w * animatedProgress.value

            drawRect(color = color.copy(alpha = trackAlpha))

            if (fillW > 0f) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.6f), color),
                        startX = 0f,
                        endX = fillW.coerceAtLeast(1f)
                    ),
                    size = Size(fillW, h)
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                        startY = 0f,
                        endY = h
                    ),
                    size = Size(fillW, h * 0.55f)
                )
                if (fillW > h) {
                    drawCircle(
                        color = color,
                        radius = h * 1.15f,
                        center = Offset(fillW, h / 2f),
                        alpha = 0.30f
                    )
                }
            }
        }
    }
}

private fun budgetColor(percentage: Float): Color = when {
    percentage >= 0.9f -> ExpenseRed
    percentage >= 0.65f -> BudgetAmber
    else -> IncomeGreen
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
