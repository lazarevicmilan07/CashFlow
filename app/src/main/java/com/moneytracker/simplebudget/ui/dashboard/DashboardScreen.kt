package com.moneytracker.simplebudget.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.domain.model.Account
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.TransactionType
import com.moneytracker.simplebudget.domain.model.CategoryBreakdown
import com.moneytracker.simplebudget.domain.model.ExpenseWithCategory
import com.moneytracker.simplebudget.ui.components.CategoryIcon
import com.moneytracker.simplebudget.ui.components.CollapsibleSummaryCard
import com.moneytracker.simplebudget.ui.components.MonthSelector
import com.moneytracker.simplebudget.ui.components.MonthYearPickerDialog
import com.moneytracker.simplebudget.ui.components.CurrencyAmountText
import com.moneytracker.simplebudget.ui.components.ScrollToTopButton
import com.moneytracker.simplebudget.ui.components.formatCurrency
import com.moneytracker.simplebudget.ui.components.formatNumber
import com.moneytracker.simplebudget.ui.components.rememberCollapseProgress
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen
import com.moneytracker.simplebudget.ui.theme.TransferBlue
import com.moneytracker.simplebudget.ui.transaction.AccountChip
import com.moneytracker.simplebudget.ui.transaction.CategoryListItem
import com.moneytracker.simplebudget.ui.transaction.SubcategoryListItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onAddTransaction: () -> Unit,
    onViewTransaction: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val symbolAfter by viewModel.currencySymbolAfter.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val selectedTransactionIds by viewModel.selectedTransactionIds.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    var showMonthPicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBulkEditTypeDialog by remember { mutableStateOf(false) }
    var showBulkDatePicker by remember { mutableStateOf(false) }
    var showBulkCategoryPicker by remember { mutableStateOf(false) }
    var showBulkAccountPicker by remember { mutableStateOf(false) }
    var showBulkNoteDialog by remember { mutableStateOf(false) }
    BackHandler(enabled = isMultiSelectMode) { viewModel.clearSelection() }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isMultiSelectMode) Text("${selectedTransactionIds.size} selected")
                    else Text("Transactions")
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { showBulkEditTypeDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit selected")
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                        }
                    } else {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    val activeCount = listOfNotNull(
                                        filter.transactionType,
                                        filter.categoryId,
                                        filter.subcategoryId,
                                        filter.accountId,
                                        filter.searchQuery.takeIf { it.isNotBlank() }
                                    ).size
                                    if (activeCount > 0) {
                                        Badge { Text("$activeCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filters")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScrollToTopButton(
                    listState = listState,
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    }
                )
                FloatingActionButton(
                    onClick = onAddTransaction,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val swipeThreshold = 100f
            val collapseProgress = rememberCollapseProgress(listState)

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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Pinned month selector
                Box(modifier = Modifier.padding(horizontal = 10.dp)) {
                    MonthSelector(
                        selectedMonth = selectedMonth,
                        onPreviousMonth = animateToPrevious,
                        onNextMonth = animateToNext,
                        onMonthClick = { showMonthPicker = true }
                    )
                }

                if (showFilterSheet) {
                    FilterBottomSheet(
                        filter = filter,
                        categories = uiState.categories,
                        accounts = uiState.accounts,
                        onTypeFilter = viewModel::setTransactionTypeFilter,
                        onCategoryFilter = viewModel::setCategoryFilter,
                        onSubcategoryFilter = viewModel::setSubcategoryFilter,
                        onAccountFilter = viewModel::setAccountFilter,
                        onSearchQuery = viewModel::setSearchQuery,
                        onClearFilters = viewModel::clearFilters,
                        onDismiss = { showFilterSheet = false }
                    )
                }

                // --- Multiselect dialogs ---

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        icon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        title = { Text("Delete Transactions") },
                        text = {
                            Text("Delete ${selectedTransactionIds.size} selected transaction(s)? This cannot be undone.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteSelectedTransactions()
                                    showDeleteConfirmDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showBulkEditTypeDialog) {
                    AlertDialog(
                        onDismissRequest = { showBulkEditTypeDialog = false },
                        title = { Text("Edit ${selectedTransactionIds.size} Transaction(s)") },
                        text = {
                            Column {
                                listOf(
                                    "Edit all dates",
                                    "Edit all categories",
                                    "Edit all accounts",
                                    "Edit all notes"
                                ).forEachIndexed { index, label ->
                                    TextButton(
                                        onClick = {
                                            showBulkEditTypeDialog = false
                                            when (index) {
                                                0 -> showBulkDatePicker = true
                                                1 -> showBulkCategoryPicker = true
                                                2 -> showBulkAccountPicker = true
                                                3 -> showBulkNoteDialog = true
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            label,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showBulkEditTypeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                if (showBulkDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = System.currentTimeMillis()
                    )
                    DatePickerDialog(
                        onDismissRequest = { showBulkDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    datePickerState.selectedDateMillis?.let { millis ->
                                        val date = Instant.ofEpochMilli(millis)
                                            .atZone(ZoneId.of("UTC"))
                                            .toLocalDate()
                                        viewModel.bulkUpdateDate(date)
                                    }
                                    showBulkDatePicker = false
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBulkDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showBulkCategoryPicker) {
                    BulkCategoryPickerSheet(
                        categories = uiState.categories,
                        onDismiss = { showBulkCategoryPicker = false },
                        onConfirm = { categoryId, subcategoryId ->
                            viewModel.bulkUpdateCategory(categoryId, subcategoryId)
                            showBulkCategoryPicker = false
                        }
                    )
                }

                if (showBulkAccountPicker) {
                    BulkAccountPickerSheet(
                        accounts = uiState.accounts,
                        onDismiss = { showBulkAccountPicker = false },
                        onConfirm = { accountId ->
                            viewModel.bulkUpdateAccount(accountId)
                            showBulkAccountPicker = false
                        }
                    )
                }

                if (showBulkNoteDialog) {
                    var noteInput by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showBulkNoteDialog = false },
                        title = { Text("Edit Note") },
                        text = {
                            OutlinedTextField(
                                value = noteInput,
                                onValueChange = { noteInput = it },
                                placeholder = { Text("Enter note for all selected transactions") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.bulkUpdateNote(noteInput)
                                    showBulkNoteDialog = false
                                }
                            ) { Text("Apply") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBulkNoteDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // Pinned collapsible hero — swipes with content
                CollapsibleSummaryCard(
                    income = uiState.monthlyStats.totalIncome,
                    expense = uiState.monthlyStats.totalExpense,
                    balance = uiState.monthlyStats.balance,
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
                                    coroutineScope.launch {
                                        dragOffset.animateTo(0f, animationSpec = tween(200))
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    coroutineScope.launch {
                                        dragOffset.snapTo(dragOffset.value + dragAmount)
                                    }
                                }
                            )
                        },
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Content with interactive drag offset
                    item {
                        Column(
                            modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) },
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Filter status
                            if (filter.isActive && filteredTransactions.isNotEmpty()) {
                                Text(
                                    text = "Showing ${filteredTransactions.size} of ${uiState.recentTransactions.size} transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            // Transactions grouped by date
                            if (filteredTransactions.isNotEmpty()) {
                                val groupedByDate = filteredTransactions
                                    .sortedByDescending { it.expense.date }
                                    .groupBy { it.expense.date }

                                groupedByDate.forEach { (date, transactions) ->
                                    Text(
                                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
                                    )

                                    transactions.forEach { transaction ->
                                        val txId = transaction.expense.id
                                        CompactTransactionItem(
                                            transaction = transaction,
                                            currency = currency,
                                            isSelected = selectedTransactionIds.contains(txId),
                                            isMultiSelectMode = isMultiSelectMode,
                                            onLongPress = { viewModel.toggleTransactionSelection(txId) },
                                            onClick = {
                                                if (isMultiSelectMode) viewModel.toggleTransactionSelection(txId)
                                                else onViewTransaction(txId)
                                            }
                                        )
                                    }
                                }
                            } else if (filter.isActive) {
                                FilteredEmptyState(onClearFilters = viewModel::clearFilters)
                            } else {
                                EmptyState()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseBreakdownCard(
    breakdown: List<CategoryBreakdown>,
    currency: String,
    symbolAfter: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Expense Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Simple Pie Chart
            SimplePieChart(
                breakdown = breakdown,
                currency = currency,
                symbolAfter = symbolAfter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            breakdown.take(5).forEach { item ->
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
                    Text(
                        text = formatCurrency(item.amount, currency, symbolAfter),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(
    breakdown: List<CategoryBreakdown>,
    currency: String,
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
                        val innerR = outerR * 0.55f
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
                val strokeWidth = outerRadius * 0.45f
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

        // Tooltip
        if (selectedIndex in breakdown.indices) {
            val item = breakdown[selectedIndex]
            Text(
                text = "${item.category?.name ?: "Uncategorized"}: ${formatCurrency(item.amount, currency, symbolAfter)} (${item.percentage.toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CompactTransactionItem(
    transaction: ExpenseWithCategory,
    currency: String,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onClick: () -> Unit
) {
    val isExpense = transaction.expense.type == TransactionType.EXPENSE
    val isTransfer = transaction.expense.type == TransactionType.TRANSFER
    val amountColor by animateColorAsState(
        targetValue = when (transaction.expense.type) {
            TransactionType.EXPENSE -> ExpenseRed
            TransactionType.INCOME -> IncomeGreen
            TransactionType.TRANSFER -> TransferBlue
        },
        label = "amount_color"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                      else MaterialTheme.colorScheme.surface,
        label = "item_bg"
    )
    val hasSubcategory = transaction.subcategory != null
    val hasNote = transaction.expense.note.isNotBlank()
    val accountName = transaction.account?.name
    val toAccountName = transaction.toAccount?.name

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            CategoryIcon(
                icon = if (isTransfer) "swap_horiz" else (transaction.subcategory?.icon ?: transaction.category?.icon ?: "more_horiz"),
                color = if (isTransfer) TransferBlue else (transaction.subcategory?.color ?: transaction.category?.color ?: Color.Gray),
                size = 30.dp,
                iconSize = 15.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Left column: category/subcategory
            Column(
                modifier = Modifier.width(110.dp),
                verticalArrangement = if (hasSubcategory && !isTransfer) Arrangement.Top else Arrangement.Center
            ) {
                Text(
                    text = if (isTransfer) "Transfer" else (transaction.category?.name ?: "Uncategorized"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isTransfer && hasSubcategory) {
                    Text(
                        text = transaction.subcategory!!.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right column: note/account
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = if (hasNote) Arrangement.Top else Arrangement.Center
            ) {
                if (hasNote) {
                    Text(
                        text = transaction.expense.note,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isTransfer && accountName != null && toAccountName != null) {
                    Text(
                        text = "$accountName -> $toAccountName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (accountName != null) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = when {
                    isTransfer -> formatNumber(transaction.expense.amount, currency)
                    isExpense -> "-${formatNumber(transaction.expense.amount, currency)}"
                    else -> "+${formatNumber(transaction.expense.amount, currency)}"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                modifier = Modifier.width(100.dp),
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Tap + to add your first transaction",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun FilteredEmptyState(onClearFilters: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No transactions match your filters",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear filters")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filter: TransactionFilter,
    categories: List<Category>,
    accounts: List<Account>,
    onTypeFilter: (TransactionType?) -> Unit,
    onCategoryFilter: (Long?) -> Unit,
    onSubcategoryFilter: (Long?) -> Unit,
    onAccountFilter: (Long?) -> Unit,
    onSearchQuery: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val parentCategories = remember(categories) { categories.filter { it.parentCategoryId == null } }
    val subcategories = remember(categories, filter.categoryId) {
        if (filter.categoryId != null) categories.filter { it.parentCategoryId == filter.categoryId } else emptyList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(8.dp))

            // Transaction type
            Text("Type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filter.transactionType == null,
                    onClick = { onTypeFilter(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filter.transactionType == TransactionType.EXPENSE,
                    onClick = { onTypeFilter(if (filter.transactionType == TransactionType.EXPENSE) null else TransactionType.EXPENSE) },
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = filter.transactionType == TransactionType.INCOME,
                    onClick = { onTypeFilter(if (filter.transactionType == TransactionType.INCOME) null else TransactionType.INCOME) },
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = filter.transactionType == TransactionType.TRANSFER,
                    onClick = { onTypeFilter(if (filter.transactionType == TransactionType.TRANSFER) null else TransactionType.TRANSFER) },
                    label = { Text("Transfer") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category
            Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            FlowChipsRow(
                items = parentCategories,
                selectedId = filter.categoryId,
                allLabel = "All",
                nameSelector = { it.name },
                idSelector = { it.id },
                onSelected = { onCategoryFilter(it) }
            )

            // Subcategory
            if (filter.categoryId != null && subcategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Subcategory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                FlowChipsRow(
                    items = subcategories,
                    selectedId = filter.subcategoryId,
                    allLabel = "All",
                    nameSelector = { it.name },
                    idSelector = { it.id },
                    onSelected = { onSubcategoryFilter(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Account
            Text("Account", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            FlowChipsRow(
                items = accounts,
                selectedId = filter.accountId,
                allLabel = "All",
                nameSelector = { it.name },
                idSelector = { it.id },
                onSelected = { onAccountFilter(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Search
            Text("Search", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = onSearchQuery,
                placeholder = { Text("Search notes...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (filter.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = { onClearFilters() }) {
                    Text("Clear")
                }
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkCategoryPickerSheet(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (categoryId: Long?, subcategoryId: Long?) -> Unit
) {
    val parentCategories = remember(categories) { categories.filter { it.parentCategoryId == null } }
    var selectedParentId by remember { mutableStateOf<Long?>(null) }
    val subcategories = remember(categories, selectedParentId) {
        if (selectedParentId != null) categories.filter { it.parentCategoryId == selectedParentId } else emptyList()
    }
    var selectedSubcategoryId by remember { mutableStateOf<Long?>(null) }

    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val typeColor = MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                color = Color.Transparent
            ) {
                val maxRows = maxOf(parentCategories.size, subcategories.size)
                val contentHeight = (maxRows * 49).dp

                Row(modifier = Modifier.heightIn(max = 340.dp)) {
                    // Left column — parent categories
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(parentCategories) { cat ->
                                val hasSubcategories = categories.any { it.parentCategoryId == cat.id }
                                CategoryListItem(
                                    category = cat,
                                    isSelected = selectedParentId == cat.id,
                                    onClick = {
                                        selectedParentId = if (selectedParentId == cat.id) null else cat.id
                                        selectedSubcategoryId = null
                                    },
                                    showArrow = hasSubcategories,
                                    typeColor = typeColor
                                )
                                HorizontalDivider(thickness = 1.dp, color = borderColor)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .width(1.dp)
                                .height(contentHeight)
                                .background(borderColor)
                        )
                    }
                    // Right column — subcategories
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(subcategories) { sub ->
                            SubcategoryListItem(
                                subcategory = sub,
                                isSelected = sub.id == selectedSubcategoryId,
                                onClick = {
                                    selectedSubcategoryId = if (selectedSubcategoryId == sub.id) null else sub.id
                                }
                            )
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(
                    onClick = { onConfirm(selectedParentId, selectedSubcategoryId) },
                    enabled = selectedParentId != null
                ) { Text("Apply") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkAccountPickerSheet(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: Long?) -> Unit
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val col0 = accounts.filterIndexed { i, _ -> i % 2 == 0 }
    val col1 = accounts.filterIndexed { i, _ -> i % 2 == 1 }
    val contentHeight = (maxOf(col0.size, col1.size) * 49).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Select Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, borderColor),
                color = Color.Transparent
            ) {
                Row(modifier = Modifier.heightIn(max = 300.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(col0) { account ->
                                AccountChip(
                                    account = account,
                                    isSelected = false,
                                    onClick = { onConfirm(account.id) }
                                )
                                HorizontalDivider(thickness = 1.dp, color = borderColor)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .width(1.dp)
                                .height(contentHeight)
                                .background(borderColor)
                        )
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(col1) { account ->
                            AccountChip(
                                account = account,
                                isSelected = false,
                                onClick = { onConfirm(account.id) }
                            )
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <T> FlowChipsRow(
    items: List<T>,
    selectedId: Long?,
    allLabel: String,
    nameSelector: (T) -> String,
    idSelector: (T) -> Long,
    onSelected: (Long?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = selectedId == null,
            onClick = { onSelected(null) },
            label = { Text(allLabel, style = MaterialTheme.typography.labelSmall) },
            modifier = Modifier.height(32.dp)
        )
        items.forEach { item ->
            val id = idSelector(item)
            FilterChip(
                selected = selectedId == id,
                onClick = { onSelected(if (selectedId == id) null else id) },
                label = { Text(nameSelector(item), style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(32.dp)
            )
        }
    }
}

