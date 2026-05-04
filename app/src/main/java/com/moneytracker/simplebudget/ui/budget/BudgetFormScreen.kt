package com.moneytracker.simplebudget.ui.budget

import android.widget.Toast
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.R
import com.moneytracker.simplebudget.domain.model.BudgetPeriod
import com.moneytracker.simplebudget.domain.model.BudgetScope
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.Budget
import com.moneytracker.simplebudget.ui.components.CategorySelectionPanel
import com.moneytracker.simplebudget.ui.components.formatCurrency
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import com.moneytracker.simplebudget.ui.transaction.AmountInputPanel
import com.moneytracker.simplebudget.ui.transaction.FormFieldRow
import com.moneytracker.simplebudget.ui.transaction.HeroAmountDisplay
import com.moneytracker.simplebudget.ui.transaction.SaveButtonsPanel
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: (() -> Unit)? = null,
    viewModel: BudgetFormViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val existingBudget by viewModel.existingBudget.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val rootCategories by viewModel.rootCategories.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val currencySymbolAfter by viewModel.currencySymbolAfter.collectAsState()
    val isEditing = viewModel.budgetId != 0L

    val initialYearMonth = YearMonth.of(viewModel.initialYear, viewModel.initialMonth)

    var isOverallBudget by remember { mutableStateOf(false) }
    var selectedParentCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedSubcategoryId by remember { mutableStateOf<Long?>(null) }
    val subcategories = remember(selectedParentCategoryId, allCategories) {
        val catId = selectedParentCategoryId ?: return@remember emptyList<Category>()
        allCategories.filter { it.parentCategoryId == catId }
    }
    var selectedPeriod by remember { mutableStateOf(viewModel.initialPeriod) }
    var selectedYearMonth by remember { mutableStateOf(initialYearMonth) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var initialised by remember { mutableStateOf(false) }
    var formResetKey by remember { mutableIntStateOf(0) }

    val formAlpha = remember { Animatable(1f) }
    val formOffsetX = remember { Animatable(0f) }

    LaunchedEffect(formResetKey) {
        if (formResetKey > 0) {
            formAlpha.snapTo(0f)
            formOffsetX.snapTo(300f)
            launch { formAlpha.animateTo(1f, tween(300)) }
            formOffsetX.animateTo(0f, tween(300, easing = EaseOut))
        }
    }

    LaunchedEffect(existingBudget) {
        val budget = existingBudget ?: return@LaunchedEffect
        if (!initialised) {
            initialised = true
            isOverallBudget = budget.categoryId == null
            selectedParentCategoryId = budget.categoryId
            selectedSubcategoryId = budget.subcategoryId
            selectedPeriod = budget.period
            selectedYearMonth = if (budget.month != null) YearMonth.of(budget.year, budget.month)
                                else YearMonth.of(budget.year, 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                BudgetFormEvent.Saved -> {
                    Toast.makeText(context, context.getString(R.string.budget_saved), Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                BudgetFormEvent.SavedAndContinue -> {
                    Toast.makeText(context, context.getString(R.string.budget_saved), Toast.LENGTH_SHORT).show()
                    isOverallBudget = false
                    selectedParentCategoryId = null
                    selectedSubcategoryId = null
                    selectedYearMonth = initialYearMonth
                    formResetKey++
                }
                BudgetFormEvent.Deleted -> {
                    Toast.makeText(context, context.getString(R.string.budget_deleted), Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is BudgetFormEvent.ValidationError -> {
                    Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    BackHandler(enabled = uiState.currentField != BudgetFormField.NONE) {
        viewModel.setCurrentField(BudgetFormField.NONE)
    }

    val selectedCategory = selectedParentCategoryId?.let { id -> allCategories.find { it.id == id } }
    val selectedSubcategory = selectedSubcategoryId?.let { id -> allCategories.find { it.id == id } }
    val displayedCategoryForRow: Category? = if (isOverallBudget) null else (selectedSubcategory ?: selectedCategory)
    val categoryDisplayText = when {
        isOverallBudget -> stringResource(R.string.budget_all_categories)
        selectedSubcategory != null -> "${selectedCategory?.name ?: ""} › ${selectedSubcategory.name}"
        selectedCategory != null -> selectedCategory.name
        else -> ""
    }

    val isMonthly = selectedPeriod == BudgetPeriod.MONTHLY
    val scopeDisplayText = stringResource(when (uiState.selectedScope) {
        BudgetScope.THIS_PERIOD_ONLY ->
            if (isMonthly) R.string.budget_scope_this_month else R.string.budget_scope_this_year
        BudgetScope.THIS_AND_3_BEFORE ->
            if (isMonthly) R.string.budget_scope_this_and_3_before_months else R.string.budget_scope_this_and_3_before_years
        BudgetScope.THIS_AND_6_BEFORE ->
            if (isMonthly) R.string.budget_scope_this_and_6_before_months else R.string.budget_scope_this_and_before_years
        BudgetScope.THIS_AND_ALL_BEFORE ->
            if (isMonthly) R.string.budget_scope_this_and_before_months else R.string.budget_scope_this_and_before_years
        BudgetScope.ALL_PERIODS ->
            if (isMonthly) R.string.budget_scope_all_months else R.string.budget_scope_all_years
        BudgetScope.THIS_AND_3_FUTURE ->
            if (isMonthly) R.string.budget_scope_this_and_3_future_months else R.string.budget_scope_this_and_3_future_years
        BudgetScope.THIS_AND_6_FUTURE ->
            if (isMonthly) R.string.budget_scope_this_and_6_future_months else R.string.budget_scope_this_and_future_years
        BudgetScope.THIS_AND_FUTURE ->
            if (isMonthly) R.string.budget_scope_this_and_future_months else R.string.budget_scope_this_and_future_years
    })

    val typeColor = MaterialTheme.colorScheme.primary

    val onSave: () -> Unit = {
        viewModel.save(selectedParentCategoryId, selectedSubcategoryId, selectedPeriod, selectedYearMonth, isOverallBudget)
    }
    val onContinue: () -> Unit = {
        viewModel.saveAndContinue(selectedParentCategoryId, selectedSubcategoryId, selectedPeriod, selectedYearMonth, isOverallBudget)
    }
    val onDelete: (() -> Unit)? = if (isEditing && existingBudget != null) {
        { showDeleteConfirm = true }
    } else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.button_cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text(
                        text = if (isEditing) stringResource(R.string.budget_edit_budget)
                               else stringResource(R.string.budget_set_budget),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val periodLabel = if (selectedPeriod == BudgetPeriod.MONTHLY)
                        "${Month.of(selectedYearMonth.monthValue).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedYearMonth.year}"
                    else
                        selectedYearMonth.year.toString()
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer { alpha = formAlpha.value }
                    .offset { IntOffset(formOffsetX.value.roundToInt(), 0) }
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                BudgetPeriodToggle(
                    selectedPeriod = selectedPeriod,
                    typeColor = typeColor,
                    onSelect = { isMonthlySelected ->
                        selectedPeriod = if (isMonthlySelected) BudgetPeriod.MONTHLY else BudgetPeriod.YEARLY
                        selectedYearMonth = initialYearMonth
                        viewModel.setScope(BudgetScope.THIS_PERIOD_ONLY)
                    }
                )

                HeroAmountDisplay(
                    amount = uiState.amountText,
                    currency = currency,
                    typeColor = typeColor,
                    isActive = uiState.currentField == BudgetFormField.AMOUNT,
                    onClick = { viewModel.setCurrentField(BudgetFormField.AMOUNT) },
                    symbolAfter = currencySymbolAfter
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    Column {
                        FormFieldRow(
                            label = stringResource(R.string.label_category),
                            value = categoryDisplayText,
                            onClick = { viewModel.setCurrentField(BudgetFormField.CATEGORY) },
                            isActive = uiState.currentField == BudgetFormField.CATEGORY,
                            typeColor = typeColor,
                            iconName = displayedCategoryForRow?.icon,
                            iconColor = displayedCategoryForRow?.color
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        FormFieldRow(
                            label = stringResource(R.string.label_period),
                            value = scopeDisplayText,
                            onClick = { viewModel.setCurrentField(BudgetFormField.SCOPE) },
                            isActive = uiState.currentField == BudgetFormField.SCOPE,
                            typeColor = typeColor
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Column {
                    when (uiState.currentField) {
                        BudgetFormField.AMOUNT -> {
                            AmountInputPanel(
                                typeColor = typeColor,
                                onDigit = viewModel::appendToAmount,
                                onDelete = viewModel::deleteLastDigit,
                                onDone = {
                                    viewModel.formatAmount()
                                    viewModel.setCurrentField(BudgetFormField.NONE)
                                },
                                onClose = { viewModel.setCurrentField(BudgetFormField.NONE) }
                            )
                            SaveButtonsPanel(
                                onSave = onSave,
                                onContinue = onContinue,
                                isEditing = isEditing,
                                onDelete = onDelete
                            )
                        }
                        BudgetFormField.CATEGORY -> {
                            CategorySelectionPanel(
                                categories = rootCategories,
                                allCategories = allCategories,
                                selectedParentCategoryId = selectedParentCategoryId,
                                subcategories = subcategories,
                                selectedSubcategoryId = selectedSubcategoryId,
                                onCategorySelected = { id ->
                                    isOverallBudget = false
                                    selectedParentCategoryId = id
                                    selectedSubcategoryId = null
                                    val hasSubs = allCategories.any { it.parentCategoryId == id }
                                    if (!hasSubs) viewModel.setCurrentField(BudgetFormField.SCOPE)
                                },
                                onSubcategorySelected = { id ->
                                    isOverallBudget = false
                                    selectedSubcategoryId = id
                                    viewModel.setCurrentField(BudgetFormField.SCOPE)
                                },
                                onParentSelected = {
                                    isOverallBudget = false
                                    selectedSubcategoryId = null
                                    viewModel.setCurrentField(BudgetFormField.SCOPE)
                                },
                                onClose = { viewModel.setCurrentField(BudgetFormField.NONE) },
                                onEditCategories = onNavigateToCategories,
                                onOverallSelected = {
                                    isOverallBudget = true
                                    selectedParentCategoryId = null
                                    selectedSubcategoryId = null
                                    viewModel.setCurrentField(BudgetFormField.SCOPE)
                                },
                                isOverallSelected = isOverallBudget
                            )
                            SaveButtonsPanel(
                                onSave = onSave,
                                onContinue = onContinue,
                                isEditing = isEditing,
                                onDelete = onDelete
                            )
                        }
                        BudgetFormField.SCOPE -> {
                            ScopeSelectionPanel(
                                isEditing = isEditing,
                                period = selectedPeriod,
                                selectedScope = uiState.selectedScope,
                                typeColor = typeColor,
                                onScopeSelected = { scope ->
                                    viewModel.setScope(scope)
                                    viewModel.setCurrentField(BudgetFormField.AMOUNT)
                                },
                                onClose = { viewModel.setCurrentField(BudgetFormField.NONE) }
                            )
                            SaveButtonsPanel(
                                onSave = onSave,
                                onContinue = onContinue,
                                isEditing = isEditing,
                                onDelete = onDelete
                            )
                        }
                        BudgetFormField.NONE -> {
                            SaveButtonsPanel(
                                onSave = onSave,
                                onContinue = onContinue,
                                isEditing = isEditing,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val budget = existingBudget
        if (budget != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text(stringResource(R.string.budget_delete_confirm_title)) },
                text = { Text(stringResource(R.string.budget_delete_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteBudget(budget)
                            showDeleteConfirm = false
                        }
                    ) { Text(stringResource(R.string.button_delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            )
        }
    }

    val pendingConflicts = uiState.pendingConflicts
    if (pendingConflicts != null) {
        BudgetConflictDialog(
            conflicts = pendingConflicts,
            newAmount = uiState.amountText.toDoubleOrNull() ?: 0.0,
            currency = currency,
            currencySymbolAfter = currencySymbolAfter,
            onConfirm = { viewModel.confirmAndSave() },
            onDismiss = { viewModel.cancelSave() }
        )
    }
}

@Composable
private fun BudgetConflictDialog(
    conflicts: List<Budget>,
    newAmount: Double,
    currency: String,
    currencySymbolAfter: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val newAmountFormatted = formatCurrency(newAmount, currency, currencySymbolAfter)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.budget_conflict_title)) },
        text = {
            Column {
                if (conflicts.size == 1) {
                    val conflict = conflicts.first()
                    val existingFormatted = formatCurrency(conflict.amount, currency, currencySymbolAfter)
                    val periodLabel = conflict.month?.let { m ->
                        "${Month.of(m).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${conflict.year}"
                    } ?: conflict.year.toString()
                    Text(stringResource(R.string.budget_conflict_message_single, existingFormatted, periodLabel, newAmountFormatted))
                } else {
                    Text(stringResource(R.string.budget_conflict_message_multiple, conflicts.size, newAmountFormatted))
                    Spacer(modifier = Modifier.height(8.dp))
                    conflicts.forEach { conflict ->
                        val existingFormatted = formatCurrency(conflict.amount, currency, currencySymbolAfter)
                        val periodLabel = conflict.month?.let { m ->
                            "${Month.of(m).getDisplayName(TextStyle.FULL, Locale.getDefault())} ${conflict.year}"
                        } ?: conflict.year.toString()
                        Text(
                            text = "• $periodLabel: $existingFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.budget_conflict_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
private fun ScopeSelectionPanel(
    isEditing: Boolean,
    period: BudgetPeriod,
    selectedScope: BudgetScope,
    typeColor: Color,
    onScopeSelected: (BudgetScope) -> Unit,
    onClose: () -> Unit
) {
    val isMonthly = period == BudgetPeriod.MONTHLY
    val topOption = if (isMonthly)
        BudgetScope.THIS_PERIOD_ONLY to R.string.budget_scope_this_month
    else
        BudgetScope.THIS_PERIOD_ONLY to R.string.budget_scope_this_year
    val beforeOptions = if (isMonthly) listOf(
        BudgetScope.THIS_AND_3_BEFORE to R.string.budget_scope_this_and_3_before_months,
        BudgetScope.THIS_AND_6_BEFORE to R.string.budget_scope_this_and_6_before_months,
        BudgetScope.THIS_AND_ALL_BEFORE to R.string.budget_scope_this_and_before_months,
    ) else listOf(
        BudgetScope.THIS_AND_3_BEFORE to R.string.budget_scope_this_and_3_before_years,
        BudgetScope.THIS_AND_ALL_BEFORE to R.string.budget_scope_this_and_before_years,
    )
    val futureOptions = if (isMonthly) listOf(
        BudgetScope.THIS_AND_3_FUTURE to R.string.budget_scope_this_and_3_future_months,
        BudgetScope.THIS_AND_6_FUTURE to R.string.budget_scope_this_and_6_future_months,
        BudgetScope.THIS_AND_FUTURE to R.string.budget_scope_this_and_future_months,
    ) else listOf(
        BudgetScope.THIS_AND_3_FUTURE to R.string.budget_scope_this_and_3_future_years,
        BudgetScope.THIS_AND_FUTURE to R.string.budget_scope_this_and_future_years,
    )
    val bottomOption = if (isMonthly)
        BudgetScope.ALL_PERIODS to R.string.budget_scope_all_months
    else
        BudgetScope.ALL_PERIODS to R.string.budget_scope_all_years
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(if (isEditing) R.string.budget_scope_title_edit else R.string.budget_scope_title_create),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            color = Color.Transparent
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ScopeChip(
                    label = stringResource(topOption.second),
                    isSelected = topOption.first == selectedScope,
                    typeColor = typeColor,
                    onClick = { onScopeSelected(topOption.first) },
                    centered = true
                )
                HorizontalDivider(thickness = 1.dp, color = borderColor)
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    Column(modifier = Modifier.weight(1f)) {
                        beforeOptions.forEachIndexed { i, (scope, labelRes) ->
                            ScopeChip(
                                label = stringResource(labelRes),
                                isSelected = scope == selectedScope,
                                typeColor = typeColor,
                                onClick = { onScopeSelected(scope) }
                            )
                            if (i < beforeOptions.lastIndex) HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(borderColor)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        futureOptions.forEachIndexed { i, (scope, labelRes) ->
                            ScopeChip(
                                label = stringResource(labelRes),
                                isSelected = scope == selectedScope,
                                typeColor = typeColor,
                                onClick = { onScopeSelected(scope) }
                            )
                            if (i < futureOptions.lastIndex) HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = borderColor)
                ScopeChip(
                    label = stringResource(bottomOption.second),
                    isSelected = bottomOption.first == selectedScope,
                    typeColor = typeColor,
                    onClick = { onScopeSelected(bottomOption.first) },
                    centered = true
                )
            }
        }
    }
}

@Composable
private fun ScopeChip(
    label: String,
    isSelected: Boolean,
    typeColor: Color,
    onClick: () -> Unit,
    centered: Boolean = false
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) typeColor.copy(alpha = 0.1f) else Color.Transparent,
        label = "scope_chip_bg"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (centered) Arrangement.Center else Arrangement.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        )
    }
}

@Composable
private fun BudgetPeriodToggle(
    selectedPeriod: BudgetPeriod,
    typeColor: Color,
    onSelect: (isMonthly: Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            listOf(BudgetPeriod.MONTHLY to true, BudgetPeriod.YEARLY to false).forEach { (period, isMonthly) ->
                val isSelected = period == selectedPeriod
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) typeColor else Color.Transparent,
                    label = "period_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "period_text"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onSelect(isMonthly) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isMonthly) "Monthly" else "Yearly",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
