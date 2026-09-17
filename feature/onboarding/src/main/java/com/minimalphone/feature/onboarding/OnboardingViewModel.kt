package com.minimalphone.feature.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.common.AccessibilityHelper
import com.minimalphone.core.common.SystemHealthHelper
import com.minimalphone.core.domain.CheckUsagePermissionUseCase
import com.minimalphone.core.domain.GetInstalledAppsUseCase
import com.minimalphone.core.domain.SetOnboardingCompletedUseCase
import com.minimalphone.core.domain.SyncInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep {
    VISION,
    DEFAULT_LAUNCHER,
    PERMISSIONS,
    SCAN_AND_READY
}

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.VISION,
    val isDefaultLauncher: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isUsageGranted: Boolean = false,
    val totalAppsCount: Int = 0,
    val essentialAppsCount: Int = 0,
    val managedAppsCount: Int = 0,
    val isScanning: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshState()
        observeDiscoveredApps()
    }

    fun refreshState() {
        viewModelScope.launch {
            val isLauncher = SystemHealthHelper.isDefaultLauncher(context)
            val isAccess = isAccessibilityEnabledInternal()
            val isUsage = checkUsagePermissionUseCase()

            _uiState.value = _uiState.value.copy(
                isDefaultLauncher = isLauncher,
                isAccessibilityEnabled = isAccess,
                isUsageGranted = isUsage
            )
        }
    }

    private fun observeDiscoveredApps() {
        viewModelScope.launch {
            getInstalledAppsUseCase().collect { apps ->
                val essential = apps.count { it.isEssential }
                val managed = apps.count { !it.isEssential }
                _uiState.value = _uiState.value.copy(
                    totalAppsCount = apps.size,
                    essentialAppsCount = essential,
                    managedAppsCount = managed
                )
            }
        }
    }

    fun nextStep() {
        val next = when (_uiState.value.currentStep) {
            OnboardingStep.VISION -> OnboardingStep.DEFAULT_LAUNCHER
            OnboardingStep.DEFAULT_LAUNCHER -> OnboardingStep.PERMISSIONS
            OnboardingStep.PERMISSIONS -> {
                scanApps()
                OnboardingStep.SCAN_AND_READY
            }
            OnboardingStep.SCAN_AND_READY -> OnboardingStep.SCAN_AND_READY
        }
        _uiState.value = _uiState.value.copy(currentStep = next)
        refreshState()
    }

    fun previousStep() {
        val prev = when (_uiState.value.currentStep) {
            OnboardingStep.VISION -> OnboardingStep.VISION
            OnboardingStep.DEFAULT_LAUNCHER -> OnboardingStep.VISION
            OnboardingStep.PERMISSIONS -> OnboardingStep.DEFAULT_LAUNCHER
            OnboardingStep.SCAN_AND_READY -> OnboardingStep.PERMISSIONS
        }
        _uiState.value = _uiState.value.copy(currentStep = prev)
        refreshState()
    }

    private fun scanApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            syncInstalledAppsUseCase()
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    fun openDefaultLauncherSettings() {
        val intent = SystemHealthHelper.createDefaultLauncherSettingsIntent(context)
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = AccessibilityHelper.createAccessibilitySettingsIntent()
        context.startActivity(intent)
    }

    fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            setOnboardingCompletedUseCase(true)
            onFinished()
        }
    }

    private fun isAccessibilityEnabledInternal(): Boolean {
        return try {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabled.contains(context.packageName)
        } catch (_: Exception) {
            false
        }
    }
}
