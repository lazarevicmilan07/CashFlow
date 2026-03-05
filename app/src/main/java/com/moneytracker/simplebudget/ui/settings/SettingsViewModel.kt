package com.moneytracker.simplebudget.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.preferences.UserPreferences
import com.moneytracker.simplebudget.domain.usecase.BackupRestoreUseCase
import com.moneytracker.simplebudget.domain.usecase.ExportPeriodParams
import com.moneytracker.simplebudget.domain.usecase.ExportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val exportUseCase: ExportUseCase,
    private val backupRestoreUseCase: BackupRestoreUseCase
) : ViewModel() {

    val userPreferences: StateFlow<UserPreferences> = preferencesManager.userPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            preferencesManager.setCurrency(currency)
        }
    }

    fun setCurrencySymbolAfter(after: Boolean) {
        viewModelScope.launch {
            preferencesManager.setCurrencySymbolAfter(after)
        }
    }

    fun showCurrencyPicker() {
        _uiState.value = _uiState.value.copy(showCurrencyPicker = true)
    }

    fun hideCurrencyPicker() {
        _uiState.value = _uiState.value.copy(showCurrencyPicker = false)
    }

    fun exportToExcel(context: Context, uri: Uri, period: ExportPeriodParams) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = exportUseCase.exportToExcel(context, uri, period)
            _uiState.value = _uiState.value.copy(isExporting = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ExportSuccess("Excel"))
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Export failed"))
            }
        }
    }

    fun exportToPdf(context: Context, uri: Uri, period: ExportPeriodParams) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = exportUseCase.exportToPdf(context, uri, period)
            _uiState.value = _uiState.value.copy(isExporting = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.ExportSuccess("PDF"))
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Export failed"))
            }
        }
    }

    fun backup(context: Context, uri: Uri, period: ExportPeriodParams) {
        viewModelScope.launch {
            // TODO: Restore premium check for release
            // if (!userPreferences.value.isPremium) {
            //     _events.emit(SettingsEvent.ShowPremiumRequired("Backup"))
            //     return@launch
            // }
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            val result = backupRestoreUseCase.backup(context, uri, period)
            _uiState.value = _uiState.value.copy(isBackingUp = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.BackupSuccess)
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Backup failed"))
            }
        }
    }

    fun restore(context: Context, uri: Uri) {
        viewModelScope.launch {
            // TODO: Restore premium check for release
            // if (!userPreferences.value.isPremium) {
            //     _events.emit(SettingsEvent.ShowPremiumRequired("Restore"))
            //     return@launch
            // }
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val result = backupRestoreUseCase.restore(context, uri)
            _uiState.value = _uiState.value.copy(isRestoring = false)
            if (result.isSuccess) {
                _events.emit(SettingsEvent.RestoreSuccess)
            } else {
                _events.emit(SettingsEvent.ShowError(result.exceptionOrNull()?.message ?: "Restore failed"))
            }
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPremium(isPremium)
        }
    }
}

data class SettingsUiState(
    val showCurrencyPicker: Boolean = false,
    val isExporting: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

sealed class SettingsEvent {
    data class ExportSuccess(val type: String) : SettingsEvent()
    data object BackupSuccess : SettingsEvent()
    data object RestoreSuccess : SettingsEvent()
    data class ShowPremiumRequired(val feature: String) : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
}
