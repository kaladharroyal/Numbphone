package com.minimalphone.feature.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.common.AccessibilityHelper
import com.minimalphone.core.common.SystemHealthHelper
import com.minimalphone.core.data.repository.AppRuleRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.boot.ObserveSystemDiagnosticsUseCase
import com.minimalphone.core.domain.boot.SystemDiagnosticsData
import com.minimalphone.core.model.SettingsGuardMode
import com.minimalphone.core.common.DeviceAdminHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeSystemDiagnosticsUseCase: ObserveSystemDiagnosticsUseCase,
    private val usageStatsRepository: UsageStatsRepository,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository
) : ViewModel() {

    private val _diagnosticsState = MutableStateFlow(SystemDiagnosticsData())
    val diagnosticsState: StateFlow<SystemDiagnosticsData> = _diagnosticsState.asStateFlow()

    init {
        refreshDiagnostics()
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val isDefaultLauncher = SystemHealthHelper.isDefaultLauncher(context)
            val isAccessibilityEnabled = isAccessibilityServiceEnabledInternal()
            val isUsageGranted = usageStatsRepository.hasUsagePermission()
            val isBatteryIgnored = SystemHealthHelper.isBatteryOptimizationIgnored(context)
            val oem = Build.MANUFACTURER
            val model = Build.MODEL
            val sdk = Build.VERSION.SDK_INT
            val guidance = SystemHealthHelper.getOemGuidance()

            val guardMode = settingsRepository.settingsGuardMode.first()
            val isGuardReady = isAccessibilityEnabled && guardMode != SettingsGuardMode.OFF

            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            val adminComponent = DeviceAdminHelper.getAdminComponentName(context)
            val isUninstallActive = dpm?.isAdminActive(adminComponent) ?: false

            val quarantinedCount = appRuleRepository.observeQuarantinedCount().first()

            observeSystemDiagnosticsUseCase(
                isDefaultLauncher = isDefaultLauncher,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isUsageAccessGranted = isUsageGranted,
                isBatteryOptimizationIgnored = isBatteryIgnored,
                oemName = oem,
                deviceModel = model,
                androidVersion = sdk,
                oemGuidance = guidance,
                isSettingsGuardReady = isGuardReady,
                isUninstallLockActive = isUninstallActive,
                unreviewedQuarantinedAppsCount = quarantinedCount
            ).collect { data ->
                _diagnosticsState.value = data
            }
        }
    }

    private fun isAccessibilityServiceEnabledInternal(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.contains(context.packageName)
        } catch (_: Exception) {
            false
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

    fun openBatterySettings() {
        val intent = SystemHealthHelper.createBatteryOptimizationSettingsIntent(context)
        context.startActivity(intent)
    }
}
