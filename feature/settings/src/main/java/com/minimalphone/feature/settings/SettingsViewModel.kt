package com.minimalphone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.domain.BackupExportResult
import com.minimalphone.core.domain.BackupImportResult
import com.minimalphone.core.domain.ExportBackupJsonUseCase
import com.minimalphone.core.domain.GetSettingsUseCase
import com.minimalphone.core.domain.ImportBackupJsonUseCase
import com.minimalphone.core.domain.SetOnboardingCompletedUseCase
import com.minimalphone.core.domain.UpdateAdaptiveFrictionUseCase
import com.minimalphone.core.domain.UpdateAppThemeUseCase
import com.minimalphone.core.domain.UpdateDefaultFocusDurationUseCase
import com.minimalphone.core.domain.UpdateLocalAnalyticsUseCase
import com.minimalphone.core.domain.UserSettings
import com.minimalphone.core.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateAdaptiveFrictionUseCase: UpdateAdaptiveFrictionUseCase,
    private val updateDefaultFocusDurationUseCase: UpdateDefaultFocusDurationUseCase,
    private val updateLocalAnalyticsUseCase: UpdateLocalAnalyticsUseCase,
    private val updateAppThemeUseCase: UpdateAppThemeUseCase,
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase,
    private val exportBackupJsonUseCase: ExportBackupJsonUseCase,
    private val importBackupJsonUseCase: ImportBackupJsonUseCase
) : ViewModel() {

    private val _settingsState = MutableStateFlow(UserSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _settingsState.value = settings
            }
        }
    }

    fun onToggleAdaptiveFriction(enabled: Boolean) {
        viewModelScope.launch {
            updateAdaptiveFrictionUseCase(enabled)
        }
    }

    fun onSelectDefaultDuration(minutes: Int) {
        viewModelScope.launch {
            updateDefaultFocusDurationUseCase(minutes)
        }
    }

    fun onToggleLocalAnalytics(enabled: Boolean) {
        viewModelScope.launch {
            updateLocalAnalyticsUseCase(enabled)
        }
    }

    fun onSelectTheme(theme: AppTheme) {
        viewModelScope.launch {
            updateAppThemeUseCase(theme)
        }
    }

    fun onExportBackup(onJsonReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val result = exportBackupJsonUseCase()
                _backupStatus.value = "Backup created: ${result.totalApps} apps, ${result.totalSessions} sessions"
                onJsonReady(result.jsonString)
            } catch (e: Exception) {
                _backupStatus.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun onImportBackup(jsonString: String, onResult: (BackupImportResult) -> Unit) {
        viewModelScope.launch {
            val result = importBackupJsonUseCase(jsonString)
            if (result.isSuccess) {
                _backupStatus.value = "Restored ${result.restoredAppsCount} app rules and ${result.restoredSessionsCount} sessions"
            } else {
                _backupStatus.value = "Import failed: ${result.errorMessage}"
            }
            onResult(result)
        }
    }

    fun clearBackupStatus() {
        _backupStatus.value = null
    }

    fun onReplayOnboarding(onNavigateToOnboarding: () -> Unit) {
        viewModelScope.launch {
            setOnboardingCompletedUseCase(false)
            onNavigateToOnboarding()
        }
    }
}
