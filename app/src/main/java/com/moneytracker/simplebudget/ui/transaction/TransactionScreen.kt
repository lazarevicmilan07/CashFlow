package com.moneytracker.simplebudget.ui.transaction

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.domain.model.TransactionType
import com.moneytracker.simplebudget.domain.model.Account
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.ui.components.CategoryIcon
import com.moneytracker.simplebudget.ui.components.formatAmountInput
import com.moneytracker.simplebudget.ui.components.formatNumber
import com.moneytracker.simplebudget.ui.components.getCurrencySymbol
import com.moneytracker.simplebudget.ui.theme.ExpenseRed
import com.moneytracker.simplebudget.ui.theme.IncomeGreen
import com.moneytracker.simplebudget.ui.theme.TransferBlue
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    onNavigateBack: () -> Unit,
    onCopyTransaction: ((Long, Boolean) -> Unit)? = null,
    onNavigateToAccounts: (() -> Unit)? = null,
    onNavigateToCategories: (() -> Unit)? = null,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val symbolAfter by viewModel.currencySymbolAfter.collectAsState()
    val rootCategories by viewModel.rootCategories.collectAsState()
    val availableSubcategories by viewModel.availableSubcategories.collectAsState()
    val allCategories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val context = LocalContext.current

    val categoryDisplayText = remember(uiState.selectedParentCategoryId, uiState.selectedCategoryId, allCategories) {
        val parentCategory = uiState.selectedParentCategoryId?.let { id -> allCategories.find { it.id == id } }
        val selectedCategory = uiState.selectedCategoryId?.let { id -> allCategories.find { it.id == id } }
        when {
            parentCategory == null && selectedCategory == null -> ""
            parentCategory != null && selectedCategory != null && parentCategory.id != selectedCategory.id ->
                "${parentCategory.name}/${selectedCategory.name}"
            parentCategory != null -> parentCategory.name
            selectedCategory != null -> selectedCategory.name
            else -> ""
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var formAnimKey by remember { mutableIntStateOf(0) }
    val formAlpha = remember { Animatable(1f) }
    val formOffsetX = remember { Animatable(0f) }

    LaunchedEffect(formAnimKey) {
        if (formAnimKey > 0) {
            formAlpha.snapTo(0f)
            formOffsetX.snapTo(300f)
            launch { formAlpha.animateTo(1f, tween(300)) }
            formOffsetX.animateTo(0f, tween(300, easing = EaseOut))
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TransactionEvent.TransactionSaved -> {
                    Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is TransactionEvent.TransactionSavedAndContinue -> {
                    Toast.makeText(context, "Transaction saved", Toast.LENGTH_SHORT).show()
                    formAnimKey++
                }
                is TransactionEvent.TransactionDeleted -> {
                    Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                }
                is TransactionEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val typeColor = when (uiState.transactionType) {
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.INCOME -> IncomeGreen
        TransactionType.TRANSFER -> TransferBlue
    }

    BackHandler(enabled = uiState.currentField != TransactionField.NONE && uiState.currentField != TransactionField.DATE) {
        viewModel.setCurrentField(TransactionField.NONE)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .imePadding()
        ) {
            // Minimal header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (uiState.isEditing) "Edit Transaction" else "New Transaction",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Scrollable form content
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = formAlpha.value
                        translationX = formOffsetX.value
                    }
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                TransactionTypeToggle(
                    selectedType = uiState.transactionType,
                    onTypeSelected = viewModel::selectTransactionType
                )

                // Hero amount display
                HeroAmountDisplay(
                    amount = uiState.amount,
                    currency = currency,
                    typeColor = typeColor,
                    isActive = uiState.currentField == TransactionField.AMOUNT,
                    onClick = { viewModel.setCurrentField(TransactionField.AMOUNT) },
                    symbolAfter = symbolAfter
                )

                // Form fields card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp
                ) {
                    TransactionFormFields(
                        uiState = uiState,
                        accounts = accounts,
                        allCategories = allCategories,
                        categoryDisplayText = categoryDisplayText,
                        typeColor = typeColor,
                        viewModel = viewModel,
                        onDateClick = { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(if (uiState.currentField == TransactionField.NOTE) 100.dp else 12.dp))
            }

            LaunchedEffect(uiState.currentField) {
                if (uiState.currentField == TransactionField.NOTE ||
                    uiState.currentField == TransactionField.AMOUNT) {
                    snapshotFlow { scrollState.maxValue }
                        .collect { maxValue ->
                            if (maxValue > 0) {
                                scrollState.scrollTo(maxValue)
                            }
                        }
                }
            }

            // Bottom panel
            BottomSelectionPanel(
                uiState = uiState,
                accounts = accounts,
                rootCategories = rootCategories,
                allCategories = allCategories,
                availableSubcategories = availableSubcategories,
                typeColor = typeColor,
                viewModel = viewModel,
                onSave = { viewModel.saveTransaction() },
                onContinue = { viewModel.saveAndContinue() },
                isEditing = uiState.isEditing,
                onCopy = if (uiState.isEditing && onCopyTransaction != null) {{ showCopyDialog = true }} else null,
                onDelete = if (uiState.isEditing) {{ showDeleteDialog = true }} else null,
                onNavigateToAccounts = onNavigateToAccounts,
                onNavigateToCategories = onNavigateToCategories
            )
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = uiState.selectedDate,
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showCopyDialog && onCopyTransaction != null) {
        val expenseId = viewModel.expenseIdForCopy
        if (expenseId != null) {
            AlertDialog(
                onDismissRequest = { showCopyDialog = false },
                title = { Text("Copy Transaction") },
                text = { Text("Which date should the copy use?") },
                confirmButton = {
                    TextButton(onClick = {
                        showCopyDialog = false
                        onCopyTransaction(expenseId, true)
                    }) {
                        Text("Today's Date")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCopyDialog = false
                        onCopyTransaction(expenseId, false)
                    }) {
                        Text("Original Date")
                    }
                }
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroAmountDisplay(
    amount: String,
    currency: String,
    typeColor: Color,
    isActive: Boolean,
    onClick: () -> Unit,
    symbolAfter: Boolean = true
) {
    val symbolText = getCurrencySymbol(currency)
    val symbolColor = if (amount.isNotEmpty()) typeColor else typeColor.copy(alpha = 0.35f)
    val symbolComposable: @Composable () -> Unit = {
        Text(
            text = symbolText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = symbolColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Top) {
            if (!symbolAfter) {
                symbolComposable()
                Spacer(modifier = Modifier.width(2.dp))
            }
            val displayAmount = when {
                amount.isEmpty() -> "0"
                isActive -> formatAmountInput(amount)
                else -> {
                    val parsed = amount.toDoubleOrNull()
                    if (parsed != null) formatNumber(parsed, currency) else amount
                }
            }
            Text(
                text = displayAmount,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = if (amount.isNotEmpty()) typeColor else typeColor.copy(alpha = 0.2f)
            )
            if (symbolAfter) {
                Spacer(modifier = Modifier.width(2.dp))
                symbolComposable()
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (isActive) 40.dp else 24.dp)
                .height(2.5.dp)
                .background(
                    color = if (isActive) typeColor else typeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(1.5.dp)
                )
        )
    }
}

@Composable
fun TransactionTypeToggle(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(modifier = Modifier.padding(3.dp)) {
            TransactionType.entries.forEach { type ->
                val isSelected = type == selectedType
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        when (type) {
                            TransactionType.EXPENSE -> ExpenseRed
                            TransactionType.INCOME -> IncomeGreen
                            TransactionType.TRANSFER -> TransferBlue
                        }
                    } else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "tab_text"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onTypeSelected(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionFormFields(
    uiState: TransactionUiState,
    accounts: List<Account>,
    allCategories: List<Category>,
    categoryDisplayText: String,
    typeColor: Color,
    viewModel: TransactionViewModel,
    onDateClick: () -> Unit
) {
    val selectedAccount = accounts.find { it.id == uiState.selectedAccountId }
    val selectedToAccount = accounts.find { it.id == uiState.toAccountId }
    val displayedCategory = (uiState.selectedCategoryId?.let { id -> allCategories.find { it.id == id } }
        ?: uiState.selectedParentCategoryId?.let { id -> allCategories.find { it.id == id } })
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val dayOfWeekFormatter = DateTimeFormatter.ofPattern("EEE")

    Column {
        // Date Field
        FormFieldRow(
            label = "Date",
            value = "${uiState.selectedDate.format(dateFormatter)} (${uiState.selectedDate.format(dayOfWeekFormatter)})",
            onClick = onDateClick,
            isActive = uiState.currentField == TransactionField.DATE,
            typeColor = typeColor
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Account / From Field
        FormFieldRow(
            label = if (uiState.transactionType == TransactionType.TRANSFER) "From" else "Account",
            value = selectedAccount?.name ?: "",
            onClick = { viewModel.setCurrentField(TransactionField.ACCOUNT) },
            isActive = uiState.currentField == TransactionField.ACCOUNT,
            typeColor = typeColor,
            iconName = selectedAccount?.icon,
            iconColor = selectedAccount?.color
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        if (uiState.transactionType == TransactionType.TRANSFER) {
            FormFieldRow(
                label = "To",
                value = selectedToAccount?.name ?: "",
                onClick = { viewModel.setCurrentField(TransactionField.TO_ACCOUNT) },
                isActive = uiState.currentField == TransactionField.TO_ACCOUNT,
                typeColor = typeColor,
                iconName = selectedToAccount?.icon,
                iconColor = selectedToAccount?.color
            )
        } else {
            FormFieldRow(
                label = "Category",
                value = categoryDisplayText,
                onClick = { viewModel.setCurrentField(TransactionField.CATEGORY) },
                isActive = uiState.currentField == TransactionField.CATEGORY || uiState.currentField == TransactionField.SUBCATEGORY,
                typeColor = typeColor,
                iconName = displayedCategory?.icon,
                iconColor = displayedCategory?.color
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        // Note Field - inline editable
        val noteFocusRequester = remember { FocusRequester() }
        val isNoteActive = uiState.currentField == TransactionField.NOTE
        val noteBgColor by animateColorAsState(
            targetValue = if (isNoteActive) typeColor.copy(alpha = 0.05f) else Color.Transparent,
            label = "note_bg"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(noteBgColor)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .clickable { viewModel.setCurrentField(TransactionField.NOTE) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Note",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNoteActive) typeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    fontWeight = FontWeight.Medium
                )
                if (isNoteActive) {
                    BasicTextField(
                        value = uiState.note,
                        onValueChange = { viewModel.updateNote(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                            .focusRequester(noteFocusRequester),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(typeColor),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.setCurrentField(TransactionField.NONE) }
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (uiState.note.isEmpty()) {
                                    Text(
                                        text = "Add a note...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (uiState.note.isNotEmpty()) uiState.note else "Add a note...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.note.isNotEmpty()) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        fontWeight = if (uiState.note.isNotEmpty()) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        LaunchedEffect(isNoteActive) {
            if (isNoteActive) {
                noteFocusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun FormFieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    typeColor: Color = Color.Transparent,
    iconName: String? = null,
    iconColor: Color? = null
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) typeColor.copy(alpha = 0.05f) else Color.Transparent,
        label = "field_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isActive) typeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconName != null && iconColor != null && value.isNotEmpty()) {
                CategoryIcon(
                    icon = iconName,
                    color = iconColor,
                    size = 24.dp,
                    iconSize = 14.dp
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = value.ifEmpty { "Select" },
                style = MaterialTheme.typography.bodySmall,
                color = if (value.isEmpty())
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                else
                    MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isActive) typeColor.copy(alpha = 0.4f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun BottomSelectionPanel(
    uiState: TransactionUiState,
    accounts: List<Account>,
    rootCategories: List<Category>,
    allCategories: List<Category>,
    availableSubcategories: List<Category>,
    typeColor: Color,
    viewModel: TransactionViewModel,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    isEditing: Boolean,
    onCopy: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onNavigateToAccounts: (() -> Unit)? = null,
    onNavigateToCategories: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp
    ) {
        Column {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 2.dp)
                    .align(Alignment.CenterHorizontally)
                    .width(32.dp)
                    .height(3.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(2.dp)
                    )
            )

            when (uiState.currentField) {
                TransactionField.ACCOUNT -> {
                    AccountSelectionPanel(
                        accounts = accounts,
                        selectedAccountId = uiState.selectedAccountId,
                        excludeAccountId = null,
                        onAccountSelected = { viewModel.selectAccount(it) },
                        onClose = { viewModel.setCurrentField(TransactionField.NONE) },
                        onEditAccounts = onNavigateToAccounts,
                        title = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Accounts"
                    )
                }
                TransactionField.TO_ACCOUNT -> {
                    AccountSelectionPanel(
                        accounts = accounts,
                        selectedAccountId = uiState.toAccountId,
                        excludeAccountId = uiState.selectedAccountId,
                        onAccountSelected = { viewModel.selectToAccount(it) },
                        onClose = { viewModel.setCurrentField(TransactionField.NONE) },
                        onEditAccounts = onNavigateToAccounts,
                        title = "To Account"
                    )
                }
                TransactionField.CATEGORY, TransactionField.SUBCATEGORY -> {
                    CategorySelectionPanel(
                        categories = rootCategories,
                        allCategories = allCategories,
                        selectedParentCategoryId = uiState.selectedParentCategoryId,
                        subcategories = availableSubcategories,
                        selectedSubcategoryId = uiState.selectedCategoryId,
                        onCategorySelected = { viewModel.selectParentCategory(it) },
                        onSubcategorySelected = { viewModel.selectSubcategory(it) },
                        onParentSelected = {
                            uiState.selectedParentCategoryId?.let { viewModel.selectParentCategoryOnly(it) }
                        },
                        onClose = { viewModel.setCurrentField(TransactionField.NONE) },
                        onEditCategories = onNavigateToCategories
                    )
                }
                TransactionField.AMOUNT -> {
                    Column {
                        AmountInputPanel(
                            typeColor = typeColor,
                            onDigit = { viewModel.appendToAmount(it) },
                            onDelete = { viewModel.deleteLastDigit() },
                            onDone = { viewModel.formatAmount(); viewModel.setCurrentField(TransactionField.NOTE) },
                            onClose = { viewModel.setCurrentField(TransactionField.NONE) }
                        )
                        SaveButtonsPanel(
                            onSave = onSave,
                            onContinue = onContinue,
                            isEditing = isEditing,
                            onCopy = onCopy,
                            onDelete = onDelete
                        )
                    }
                }
                TransactionField.NOTE -> {
                    SaveButtonsPanel(
                        onSave = onSave,
                        onContinue = onContinue,
                        isEditing = isEditing,
                        onCopy = onCopy,
                        onDelete = onDelete
                    )
                }
                else -> {
                    SaveButtonsPanel(
                        onSave = onSave,
                        onContinue = onContinue,
                        isEditing = isEditing,
                        onCopy = onCopy,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSelectionPanel(
    accounts: List<Account>,
    selectedAccountId: Long?,
    excludeAccountId: Long?,
    onAccountSelected: (Long) -> Unit,
    onClose: () -> Unit,
    onEditAccounts: (() -> Unit)? = null,
    title: String
) {
    val filteredAccounts = accounts.filter { it.id != excludeAccountId }
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                IconButton(onClick = { onEditAccounts?.invoke() }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2-column grid with borders
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            color = Color.Transparent
        ) {
            val col0 = filteredAccounts.filterIndexed { i, _ -> i % 2 == 0 }
            val col1 = filteredAccounts.filterIndexed { i, _ -> i % 2 == 1 }
            val maxRows = maxOf(col0.size, col1.size)
            val contentHeight = (maxRows * 49).dp // 48dp row + 1dp divider

            Row(modifier = Modifier.heightIn(min = 300.dp, max = 300.dp)) {
                // Column 1
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(col0) { account ->
                            AccountChip(
                                account = account,
                                isSelected = account.id == selectedAccountId,
                                onClick = { onAccountSelected(account.id) }
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
                // Column 2
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(col1) { account ->
                        AccountChip(
                            account = account,
                            isSelected = account.id == selectedAccountId,
                            onClick = { onAccountSelected(account.id) }
                        )
                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountChip(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                      else Color.Transparent,
        label = "chip_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        CategoryIcon(
            icon = account.icon,
            color = account.color,
            size = 28.dp,
            iconSize = 16.dp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = account.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CategorySelectionPanel(
    categories: List<Category>,
    allCategories: List<Category>,
    selectedParentCategoryId: Long?,
    subcategories: List<Category>,
    selectedSubcategoryId: Long?,
    onCategorySelected: (Long) -> Unit,
    onSubcategorySelected: (Long) -> Unit,
    onParentSelected: () -> Unit,
    onClose: () -> Unit,
    onEditCategories: (() -> Unit)? = null
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val typeColor = ExpenseRed

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                IconButton(onClick = { onEditCategories?.invoke() }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Two-column grid with borders
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, borderColor),
            color = Color.Transparent
        ) {
            val maxRows = maxOf(categories.size, subcategories.size)
            val contentHeight = (maxRows * 49).dp // 48dp row + 1dp divider

            Row(modifier = Modifier.heightIn(max = 300.dp)) {
                // Left column - Parent categories with right border
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(categories) { category ->
                            val isSelected = category.id == selectedParentCategoryId
                            val hasSubcategories = allCategories.any { it.parentCategoryId == category.id }

                            CategoryListItem(
                                category = category,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        onParentSelected()
                                    } else {
                                        onCategorySelected(category.id)
                                    }
                                },
                                showArrow = hasSubcategories,
                                typeColor = typeColor
                            )
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }
                    }
                    // Vertical divider on right edge - only covers content rows
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .width(1.dp)
                            .height(contentHeight)
                            .background(borderColor)
                    )
                }

                // Right column - Subcategories
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(subcategories) { subcategory ->
                        SubcategoryListItem(
                            subcategory = subcategory,
                            isSelected = subcategory.id == selectedSubcategoryId,
                            onClick = { onSubcategorySelected(subcategory.id) }
                        )
                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    showArrow: Boolean = false,
    typeColor: Color = Color.Transparent
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) typeColor.copy(alpha = 0.1f) else Color.Transparent,
        label = "cat_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = category.icon,
            color = category.color,
            size = 28.dp,
            iconSize = 16.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) typeColor else MaterialTheme.colorScheme.onSurface
        )
        if (showArrow) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) typeColor.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun SubcategoryListItem(
    subcategory: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
        label = "subcat_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = subcategory.icon,
            color = subcategory.color,
            size = 28.dp,
            iconSize = 16.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = subcategory.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AmountInputPanel(
    typeColor: Color,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Modern numeric keypad - left 3 columns + right action column
        val rowHeight = 46.dp
        val gap = 4.dp

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            // Left side: digit keys (3 columns x 4 rows)
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumPadKey(text = "1", modifier = Modifier.weight(1f)) { onDigit("1") }
                    NumPadKey(text = "2", modifier = Modifier.weight(1f)) { onDigit("2") }
                    NumPadKey(text = "3", modifier = Modifier.weight(1f)) { onDigit("3") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumPadKey(text = "4", modifier = Modifier.weight(1f)) { onDigit("4") }
                    NumPadKey(text = "5", modifier = Modifier.weight(1f)) { onDigit("5") }
                    NumPadKey(text = "6", modifier = Modifier.weight(1f)) { onDigit("6") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumPadKey(text = "7", modifier = Modifier.weight(1f)) { onDigit("7") }
                    NumPadKey(text = "8", modifier = Modifier.weight(1f)) { onDigit("8") }
                    NumPadKey(text = "9", modifier = Modifier.weight(1f)) { onDigit("9") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    NumPadKey(text = "0", modifier = Modifier.weight(2f)) { onDigit("0") }
                    NumPadKey(text = ".", modifier = Modifier.weight(1f)) { onDigit(".") }
                }
            }

            // Right side: Delete (2 rows) + Done (2 rows)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                // Delete button - spans 2 rows
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * 2 + gap)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ExpenseRed.copy(alpha = 0.1f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete",
                        modifier = Modifier.size(22.dp),
                        tint = ExpenseRed
                    )
                }
                // Done/Check button - spans 2 rows
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight * 2 + gap)
                        .clip(RoundedCornerShape(14.dp))
                        .background(IncomeGreen.copy(alpha = 0.1f))
                        .clickable(onClick = onDone),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(24.dp),
                        tint = IncomeGreen
                    )
                }
            }
        }
    }
}

@Composable
fun NumPadKey(
    text: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
        } else if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun SaveButtonsPanel(
    onSave: () -> Unit,
    onContinue: () -> Unit,
    isEditing: Boolean,
    onCopy: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Copy Button (editing only)
        if (onCopy != null) {
            FilledTonalButton(
                onClick = onCopy,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TransferBlue.copy(alpha = 0.1f),
                    contentColor = TransferBlue
                )
            ) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }

        // Delete Button (editing only)
        if (onDelete != null) {
            FilledTonalButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ExpenseRed.copy(alpha = 0.1f),
                    contentColor = ExpenseRed
                )
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Delete", fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }

        // Save Button - green tinted background
        FilledTonalButton(
            onClick = onSave,
            modifier = Modifier.weight(1f).height(40.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = IncomeGreen.copy(alpha = 0.1f),
                contentColor = IncomeGreen
            )
        ) {
            Icon(Icons.Outlined.Save, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Save", fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

        // Continue Button (new transactions only)
        if (!isEditing) {
            FilledTonalButton(
                onClick = onContinue,
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = TransferBlue.copy(alpha = 0.1f),
                    contentColor = TransferBlue
                )
            ) {
                Icon(Icons.Outlined.PlayCircleOutline, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Continue", fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onDateSelected(date)
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
