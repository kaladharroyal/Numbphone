package com.minimalphone.feature.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.common.AccessibilityHelper
import com.minimalphone.core.common.SystemHealthHelper
import com.minimalphone.core.domain.CheckUsagePermissionUseCase
import com.minimalphone.core.domain.boot.ObserveSystemDiagnosticsUseCase
import com.minimalphone.core.domain.boot.SystemDiagnosticsData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeSystemDiagnosticsUseCase: ObserveSystemDiagnosticsUseCase,
    private val checkUsagePermissionUseCase: CheckUsagePermissionUseCase
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
            val isUsageGranted = checkUsagePermissionUseCase()
            val isBatteryIgnored = SystemHealthHelper.isBatteryOptimizationIgnored(context)
            val oem = Build.MANUFACTURER
            val model = Build.MODEL
            val sdk = Build.VERSION.SDK_INT
            val guidance = SystemHealthHelper.getOemGuidance()

            observeSystemDiagnosticsUseCase(
                isDefaultLauncher = isDefaultLauncher,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isUsageAccessGranted = isUsageGranted,
                isBatteryOptimizationIgnored = isBatteryIgnored,
                oemName = oem,
                deviceModel = model,
                androidVersion = sdk,
                oemGuidance = guidance
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
