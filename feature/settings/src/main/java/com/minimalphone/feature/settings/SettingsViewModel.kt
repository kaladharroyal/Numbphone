package com.minimalphone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.domain.BackupExportResult
import com.minimalphone.core.domain.BackupImportResult
import com.minimalphone.core.domain.ExportBackupJsonUseCase
import com.minimalphone.core.domain.GetSettingsUseCase
import com.minimalphone.core.domain.ImportBackupJsonUseCase
import com.minimalphone.core.domain.SetOnboardingCompletedUseCase
import com.minimalphone.core.domain.UpdateAdaptiveFrictionUseCase
import com.minimalphone.core.domain.UpdateAppThemeUseCase
import com.minimalphone.core.domain.UpdateDefaultFocusDurationUseCase
import com.minimalphone.core.domain.UpdateDumbModeUseCase
import com.minimalphone.core.domain.UpdateLocalAnalyticsUseCase
import com.minimalphone.core.domain.UserSettings
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.EssentialApp
import com.minimalphone.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateAdaptiveFrictionUseCase: UpdateAdaptiveFrictionUseCase,
    private val updateDefaultFocusDurationUseCase: UpdateDefaultFocusDurationUseCase,
    private val updateLocalAnalyticsUseCase: UpdateLocalAnalyticsUseCase,
    private val updateAppThemeUseCase: UpdateAppThemeUseCase,
    private val updateDumbModeUseCase: UpdateDumbModeUseCase,
    private val updateAutoGrayscaleInFocusUseCase: com.minimalphone.core.domain.UpdateAutoGrayscaleInFocusUseCase,
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase,
    private val exportBackupJsonUseCase: ExportBackupJsonUseCase,
    private val importBackupJsonUseCase: ImportBackupJsonUseCase,
    private val essentialAppRepository: EssentialAppRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _settingsState = MutableStateFlow(UserSettings())
    val settingsState: StateFlow<UserSettings> = _settingsState.asStateFlow()

    private val _backupStatus = MutableStateFlow<String?>(null)
    val backupStatus: StateFlow<String?> = _backupStatus.asStateFlow()

    /** Live list of user-configured essential apps for the Essential Access UI. */
    val essentialApps: StateFlow<List<EssentialApp>> =
        essentialAppRepository.observeEssentialApps()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Live list of all configured daily time limits. */
    val timeLimits: StateFlow<List<AppTimeLimit>> =
        appTimeLimitRepository.observeAllLimits()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Installed apps for setting up new limits. */
    val allInstalledApps: StateFlow<List<InstalledApp>> =
        appRepository.getAllApps()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

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

    fun onToggleDumbMode(enabled: Boolean) {
        viewModelScope.launch {
            updateDumbModeUseCase(enabled)
        }
    }

    fun onToggleAutoGrayscaleInFocus(enabled: Boolean) {
        viewModelScope.launch {
            updateAutoGrayscaleInFocusUseCase(enabled)
        }
    }

    fun onSetTimeLimit(packageName: String, limitMinutes: Int, pinToHome: Boolean = false) {
        viewModelScope.launch {
            appTimeLimitRepository.setLimit(packageName, limitMinutes, isEnabled = true)
            if (pinToHome) {
                appRepository.toggleFavoriteOnHome(packageName, true)
            }
        }
    }

    fun onRemoveTimeLimit(packageName: String) {
        viewModelScope.launch {
            appTimeLimitRepository.removeLimit(packageName)
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

    /** Remove a user-added essential app. System defaults are protected at the DAO level. */
    fun onRemoveEssentialApp(packageName: String) {
        viewModelScope.launch {
            essentialAppRepository.removeEssentialApp(packageName)
        }
    }
}
