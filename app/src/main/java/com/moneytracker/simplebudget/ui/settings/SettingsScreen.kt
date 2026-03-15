package com.moneytracker.simplebudget.ui.settings

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.data.preferences.ThemeMode
import com.moneytracker.simplebudget.domain.usecase.ExportPeriodParams
import com.moneytracker.simplebudget.notifications.MonthlyReminderOption
import com.moneytracker.simplebudget.notifications.ReminderFrequency
import com.moneytracker.simplebudget.notifications.ReminderSettings
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

enum class PendingExportAction {
    EXCEL, PDF, BACKUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    onShowPremium: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferences.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val context = LocalContext.current

    // Track which action is pending and the selected period
    var pendingAction by remember { mutableStateOf<PendingExportAction?>(null) }
    var selectedPeriod by remember { mutableStateOf<ExportPeriod?>(null) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var periodDialogTitle by remember { mutableStateOf("") }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }
    var showReminderDayOfWeekPicker by remember { mutableStateOf(false) }
    var showReminderDayOfMonthPicker by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true)
        else Toast.makeText(context, "Notification permission is required for reminders", Toast.LENGTH_SHORT).show()
    }

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            selectedPeriod?.let { period ->
                viewModel.exportToExcel(
                    context,
                    it,
                    ExportPeriodParams(period.year, period.month)
                )
            }
        }
        selectedPeriod = null
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            selectedPeriod?.let { period ->
                viewModel.exportToPdf(
                    context,
                    it,
                    ExportPeriodParams(period.year, period.month)
                )
            }
        }
        selectedPeriod = null
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            selectedPeriod?.let { period ->
                viewModel.backup(
                    context,
                    it,
                    ExportPeriodParams(period.year, period.month)
                )
            }
        }
        selectedPeriod = null
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restore(context, it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ExportSuccess -> {
                    Toast.makeText(context, "${event.type} exported successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.BackupSuccess -> {
                    Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.RestoreSuccess -> {
                    Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.ShowPremiumRequired -> {
                    onShowPremium()
                }
                is SettingsEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle period selection result
    LaunchedEffect(selectedPeriod, pendingAction) {
        if (selectedPeriod != null && pendingAction != null) {
            when (pendingAction) {
                PendingExportAction.EXCEL -> {
                    excelExportLauncher.launch(selectedPeriod!!.getFileName("expenses", "xlsx"))
                }
                PendingExportAction.PDF -> {
                    pdfExportLauncher.launch(selectedPeriod!!.getFileName("expense_report", "pdf"))
                }
                PendingExportAction.BACKUP -> {
                    backupLauncher.launch(selectedPeriod!!.getFileName("backup", "json"))
                }
                null -> {}
            }
            pendingAction = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Premium Banner
                // TODO: Restore for release - navigate to premium purchase screen
                // if (!userPreferences.isPremium) {
                //     item {
                //         PremiumBanner(onClick = onShowPremium)
                //     }
                // }
                if (!userPreferences.isPremium) {
                    item {
                        PremiumBanner(onClick = {
                            viewModel.setPremium(true)
                            Toast.makeText(context, "Premium activated! All features unlocked.", Toast.LENGTH_SHORT).show()
                        })
                    }
                }

                // Appearance Section
                item {
                    SettingsSectionHeader("Appearance")
                }

                item {
                    ListItem(
                        headlineContent = { Text("Theme") },
                        leadingContent = {
                            Icon(
                                Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        trailingContent = {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(2.dp)
                            ) {
                                ThemeMode.entries.forEach { mode ->
                                    val isSelected = userPreferences.themeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.setThemeMode(mode) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (mode) {
                                                ThemeMode.SYSTEM -> "Auto"
                                                ThemeMode.LIGHT -> "Light"
                                                ThemeMode.DARK -> "Dark"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = userPreferences.currency,
                        onClick = { viewModel.showCurrencyPicker() }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency Sign Position",
                        subtitle = run {
                            val sign = com.moneytracker.simplebudget.ui.components.getCurrencySymbol(userPreferences.currency)
                            if (userPreferences.currencySymbolAfter) "After amount (100$sign)" else "Before amount (${sign}100)"
                        },
                        onClick = { viewModel.setCurrencySymbolAfter(!userPreferences.currencySymbolAfter) }
                    )
                }

                // Notifications Section
                item {
                    SettingsSectionHeader("Notifications")
                }

                item {
                    SettingsSwitch(
                        icon = Icons.Default.Notifications,
                        title = "Transaction Reminders",
                        subtitle = if (reminderSettings.enabled)
                            "Tap below to configure schedule"
                        else
                            "Get reminded to log your transactions",
                        checked = reminderSettings.enabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setReminderEnabled(true)
                                }
                            } else {
                                viewModel.setReminderEnabled(false)
                            }
                        }
                    )
                }

                if (reminderSettings.enabled) {
                    item {
                        ReminderFrequencyRow(
                            selected = reminderSettings.frequency,
                            onSelect = { freq ->
                                viewModel.updateReminderSettings(reminderSettings.copy(frequency = freq))
                            }
                        )
                    }

                    item {
                        SettingsItem(
                            icon = Icons.Default.Schedule,
                            title = "Remind at",
                            subtitle = "%02d:%02d".format(reminderSettings.hour, reminderSettings.minute),
                            onClick = { showReminderTimePicker = true }
                        )
                    }

                    if (reminderSettings.frequency == ReminderFrequency.WEEKLY) {
                        item {
                            SettingsItem(
                                icon = Icons.Default.DateRange,
                                title = "Day of week",
                                subtitle = DayOfWeek.of(reminderSettings.dayOfWeek)
                                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                                onClick = { showReminderDayOfWeekPicker = true }
                            )
                        }
                    }

                    if (reminderSettings.frequency == ReminderFrequency.MONTHLY) {
                        item {
                            SettingsItem(
                                icon = Icons.Default.CalendarMonth,
                                title = "Day of month",
                                subtitle = reminderSettings.monthlyOption.label,
                                onClick = { showReminderDayOfMonthPicker = true }
                            )
                        }
                    }
                }

                // Data Section
                item {
                    SettingsSectionHeader("Data")
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.GridOn,
                        title = "Export to Excel",
                        subtitle = "Export data to Excel",
                        onClick = {
                            pendingAction = PendingExportAction.EXCEL
                            periodDialogTitle = "Export to Excel"
                            showPeriodDialog = true
                        }
                    )
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.PictureAsPdf,
                        title = "Export to PDF",
                        subtitle = "Generate data report",
                        onClick = {
                            pendingAction = PendingExportAction.PDF
                            periodDialogTitle = "Export to PDF"
                            showPeriodDialog = true
                        }
                    )
                }

                // TODO: Restore premium gating for release
                // item {
                //     SettingsItem(
                //         icon = Icons.Default.Backup,
                //         title = "Backup",
                //         subtitle = if (userPreferences.isPremium) "Create data backup (JSON)" else "Premium feature",
                //         onClick = {
                //             if (userPreferences.isPremium) {
                //                 pendingAction = PendingExportAction.BACKUP
                //                 periodDialogTitle = "Backup Data"
                //                 showPeriodDialog = true
                //             } else {
                //                 onShowPremium()
                //             }
                //         },
                //         isPremium = !userPreferences.isPremium
                //     )
                // }
                item {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Backup",
                        subtitle = "Create data backup (JSON)",
                        onClick = {
                            pendingAction = PendingExportAction.BACKUP
                            periodDialogTitle = "Backup Data"
                            showPeriodDialog = true
                        }
                    )
                }

                // TODO: Restore premium gating for release
                // item {
                //     SettingsItem(
                //         icon = Icons.Default.Restore,
                //         title = "Restore",
                //         subtitle = if (userPreferences.isPremium) "Restore from backup (JSON)" else "Premium feature",
                //         onClick = {
                //             if (userPreferences.isPremium) {
                //                 showRestoreConfirmDialog = true
                //             } else {
                //                 onShowPremium()
                //             }
                //         },
                //         isPremium = !userPreferences.isPremium
                //     )
                // }
                item {
                    SettingsItem(
                        icon = Icons.Default.Restore,
                        title = "Restore",
                        subtitle = "Restore from backup (JSON)",
                        onClick = {
                            showRestoreConfirmDialog = true
                        }
                    )
                }

                // About Section
                item {
                    SettingsSectionHeader("About")
                }

                item {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version 2.0.0",
                        onClick = { showAboutDialog = true }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

        }
    }

    // Period Selection Dialog
    if (showPeriodDialog) {
        PeriodSelectionDialog(
            title = periodDialogTitle,
            onDismiss = {
                showPeriodDialog = false
                pendingAction = null
            },
            onPeriodSelected = { period ->
                showPeriodDialog = false
                selectedPeriod = period
            }
        )
    }

    // Currency Picker Dialog
    if (uiState.showCurrencyPicker) {
        CurrencyPickerDialog(
            currentCurrency = userPreferences.currency,
            onCurrencySelected = { currency ->
                viewModel.setCurrency(currency)
                viewModel.hideCurrencyPicker()
            },
            onDismiss = { viewModel.hideCurrencyPicker() }
        )
    }

    // Restore Confirmation Dialog
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Restore Data") },
            text = {
                Text(
                    "This will permanently delete all your current data including transactions, categories, subcategories, and accounts, and replace it with the data from the backup file.\n\nThis action cannot be undone. Are you sure you want to continue?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirmDialog = false
                        restoreLauncher.launch(arrayOf("application/json"))
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Money Tracker - Simple Budget",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Version 2.0.0",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\u00A9 ${java.time.Year.now().value} Money Tracker - Simple Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://lazarevicmilan07.github.io/money-tracker/privacy-policy.html")
                            )
                            context.startActivity(intent)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: android.content.ActivityNotFoundException) {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                ) {
                    Text("Rate App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Reminder Time Picker
    if (showReminderTimePicker) {
        ReminderTimePickerDialog(
            initialHour = reminderSettings.hour,
            initialMinute = reminderSettings.minute,
            onConfirm = { hour, minute ->
                showReminderTimePicker = false
                viewModel.updateReminderSettings(reminderSettings.copy(hour = hour, minute = minute))
            },
            onDismiss = { showReminderTimePicker = false }
        )
    }

    // Reminder Day of Week Picker
    if (showReminderDayOfWeekPicker) {
        ReminderDayOfWeekDialog(
            selected = reminderSettings.dayOfWeek,
            onConfirm = { day ->
                showReminderDayOfWeekPicker = false
                viewModel.updateReminderSettings(reminderSettings.copy(dayOfWeek = day))
            },
            onDismiss = { showReminderDayOfWeekPicker = false }
        )
    }

    // Reminder Day of Month Picker
    if (showReminderDayOfMonthPicker) {
        ReminderDayOfMonthDialog(
            selected = reminderSettings.monthlyOption,
            onConfirm = { option ->
                showReminderDayOfMonthPicker = false
                viewModel.updateReminderSettings(reminderSettings.copy(monthlyOption = option))
            },
            onDismiss = { showReminderDayOfMonthPicker = false }
        )
    }

    // Loading Overlay
    if (uiState.isExporting || uiState.isBackingUp || uiState.isRestoring) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = when {
                            uiState.isExporting -> "Exporting..."
                            uiState.isBackingUp -> "Creating backup..."
                            else -> "Restoring..."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Upgrade to Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Unlock all features with a one-time purchase",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            if (isPremium) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
fun CurrencyPickerDialog(
    currentCurrency: String,
    onCurrencySelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val currencies = listOf(
        // Major
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "GBP" to "British Pound",
        "JPY" to "Japanese Yen",
        "CHF" to "Swiss Franc",
        // Americas
        "CAD" to "Canadian Dollar",
        "MXN" to "Mexican Peso",
        "BRL" to "Brazilian Real",
        "ARS" to "Argentine Peso",
        "CLP" to "Chilean Peso",
        "COP" to "Colombian Peso",
        "PEN" to "Peruvian Sol",
        "UYU" to "Uruguayan Peso",
        // Asia-Pacific
        "CNY" to "Chinese Yuan",
        "INR" to "Indian Rupee",
        "KRW" to "South Korean Won",
        "AUD" to "Australian Dollar",
        "NZD" to "New Zealand Dollar",
        "SGD" to "Singapore Dollar",
        "HKD" to "Hong Kong Dollar",
        "TWD" to "New Taiwan Dollar",
        "THB" to "Thai Baht",
        "MYR" to "Malaysian Ringgit",
        "IDR" to "Indonesian Rupiah",
        "PHP" to "Philippine Peso",
        "VND" to "Vietnamese Dong",
        "PKR" to "Pakistani Rupee",
        "BDT" to "Bangladeshi Taka",
        "LKR" to "Sri Lankan Rupee",
        // Europe (non-Euro)
        "SEK" to "Swedish Krona",
        "NOK" to "Norwegian Krone",
        "DKK" to "Danish Krone",
        "PLN" to "Polish Zloty",
        "CZK" to "Czech Koruna",
        "HUF" to "Hungarian Forint",
        "RON" to "Romanian Leu",
        "BGN" to "Bulgarian Lev",
        "HRK" to "Croatian Kuna",
        "RSD" to "Serbian Dinar",
        "UAH" to "Ukrainian Hryvnia",
        "ISK" to "Icelandic Krona",
        "TRY" to "Turkish Lira",
        "RUB" to "Russian Ruble",
        "GEL" to "Georgian Lari",
        // Middle East & Africa
        "ILS" to "Israeli Shekel",
        "AED" to "UAE Dirham",
        "SAR" to "Saudi Riyal",
        "QAR" to "Qatari Riyal",
        "KWD" to "Kuwaiti Dinar",
        "BHD" to "Bahraini Dinar",
        "OMR" to "Omani Rial",
        "JOD" to "Jordanian Dinar",
        "EGP" to "Egyptian Pound",
        "MAD" to "Moroccan Dirham",
        "ZAR" to "South African Rand",
        "NGN" to "Nigerian Naira",
        "KES" to "Kenyan Shilling",
        "GHS" to "Ghanaian Cedi",
        "TZS" to "Tanzanian Shilling"
    )

    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter { (code, name) ->
            code.contains(searchQuery, ignoreCase = true) ||
                name.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Currency") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currency...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn {
                items(filteredCurrencies) { (code, name) ->
                    ListItem(
                        headlineContent = { Text(code) },
                        supportingContent = { Text(name) },
                        trailingContent = {
                            if (code == currentCurrency) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onCurrencySelected(code) }
                    )
                }
            }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReminderFrequencyRow(
    selected: ReminderFrequency,
    onSelect: (ReminderFrequency) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReminderFrequency.entries.forEach { freq ->
            val isSelected = freq == selected
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(freq) },
                color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (freq) {
                        ReminderFrequency.DAILY   -> "Daily"
                        ReminderFrequency.WEEKLY  -> "Weekly"
                        ReminderFrequency.MONTHLY -> "Monthly"
                    },
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by rememberSaveable { mutableIntStateOf(initialHour) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(initialMinute) }
    val minutesFocusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Card(
            shape = RoundedCornerShape(28.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = androidx.compose.material3.CardDefaults.cardElevation(0.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Reminder Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeScrollColumn(
                        value = selectedHour,
                        range = 0..23,
                        onValueChange = { selectedHour = it },
                        onDone = { minutesFocusRequester.requestFocus() },
                        imeAction = ImeAction.Next
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TimeScrollColumn(
                        value = selectedMinute,
                        range = 0..59,
                        onValueChange = { selectedMinute = it },
                        focusRequester = minutesFocusRequester
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedHour, selectedMinute) }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeScrollColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onDone: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Done,
    focusRequester: FocusRequester? = null
) {
    val count = range.count()
    val multiplier = 200
    val totalItems = count * multiplier
    val itemHeightDp = 52.dp

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (multiplier / 2) * count + (value - range.first) - 1
    )
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val scope = rememberCoroutineScope()

    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    val internalFocusRequester = remember { FocusRequester() }
    val actualFocusRequester = focusRequester ?: internalFocusRequester
    val focusManager = LocalFocusManager.current

    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }
    val selectedAbsoluteIndex by remember(itemHeightPx) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > itemHeightPx / 2f) listState.firstVisibleItemIndex + 2
            else listState.firstVisibleItemIndex + 1
        }
    }

    LaunchedEffect(selectedAbsoluteIndex) {
        onValueChange(range.first + (selectedAbsoluteIndex % count))
    }

    BackHandler(enabled = isEditing) {
        focusManager.clearFocus()
    }

    // When the keyboard is dismissed via the hide-keyboard button (not back),
    // focus stays but IME goes away — clear focus so the drum becomes scrollable again.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (!imeVisible) focusManager.clearFocus()
    }

    val bgColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.width(80.dp).height(itemHeightDp * 3)) {
        // Always mounted so keyboard never loses its target during focus transfer.
        // Zero-sized and invisible when not editing.
        BasicTextField(
            value = editText,
            onValueChange = { text ->
                if (text.length <= 2 && text.all { it.isDigit() }) {
                    val effective = if (text.length == 2) {
                        val n = text.toInt()
                        if (n > range.last) "%02d".format(range.last) else text
                    } else text
                    editText = effective
                    effective.toIntOrNull()?.let { num -> if (num in range) onValueChange(num) }
                    if (onDone != null) {
                        val shouldAdvance = effective.length == 2 ||
                            (effective.length == 1 && (effective.toIntOrNull() ?: 0) * 10 > range.last)
                        if (shouldAdvance) {
                            effective.toIntOrNull()?.let { num ->
                                if (num in range) {
                                    val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                                    scope.launch { listState.scrollToItem(targetFirst) }
                                }
                            }
                            // Invoke onDone (e.g. requestFocus on minutes) — don't touch isEditing here;
                            // onFocusChanged will update it once focus actually moves.
                            onDone.invoke()
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val num = editText.toIntOrNull()
                    if (num != null && num in range) {
                        onValueChange(num)
                        val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                        scope.launch { listState.scrollToItem(targetFirst) }
                    }
                    focusManager.clearFocus()
                    onDone?.invoke()
                },
                onNext = {
                    val num = editText.toIntOrNull()
                    if (num != null && num in range) {
                        onValueChange(num)
                        val targetFirst = (multiplier / 2) * count + (num - range.first) - 1
                        scope.launch { listState.scrollToItem(targetFirst) }
                    }
                    onDone?.invoke()
                }
            ),
            modifier = Modifier
                .then(
                    if (isEditing) Modifier.align(Alignment.Center)
                    else Modifier.size(1.dp).alpha(0f)
                )
                .focusRequester(actualFocusRequester)
                .onFocusChanged { focusState ->
                    isEditing = focusState.isFocused
                    if (!focusState.isFocused) editText = ""
                },
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = primary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .size(80.dp, itemHeightDp)
                            .background(primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (editText.isEmpty()) {
                            Text(
                                text = "%02d".format(value),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = primary.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                        innerTextField()
                    }
                } else {
                    innerTextField()
                }
            }
        )

        if (!isEditing) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(bgColor, Color.Transparent),
                                startY = 0f, endY = size.height / 3f
                            )
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, bgColor),
                                startY = size.height * 2f / 3f, endY = size.height
                            )
                        )
                    }
            ) {
                items(totalItems) { absoluteIndex ->
                    val itemValue = range.first + (absoluteIndex % count)
                    val isSelected = absoluteIndex == selectedAbsoluteIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeightDp)
                            .then(
                                if (isSelected) Modifier.background(
                                    primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)
                                ) else Modifier
                            )
                            .clickable {
                                if (isSelected) {
                                    editText = ""
                                    actualFocusRequester.requestFocus()
                                } else {
                                    scope.launch { listState.animateScrollToItem(absoluteIndex - 1) }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "%02d".format(itemValue),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primary else onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderDayOfWeekDialog(
    selected: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day of week") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..7).forEach { day ->
                    val isSelected = day == current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { current = day },
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReminderDayOfMonthDialog(
    selected: MonthlyReminderOption,
    onConfirm: (MonthlyReminderOption) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Day of month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MonthlyReminderOption.entries.forEach { option ->
                    val isSelected = option == current
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { current = option },
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
