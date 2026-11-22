package com.minimalphone.feature.settings

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.AppRuleRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.QuarantineHours
import com.minimalphone.core.model.SettingsGuardMode
import com.minimalphone.core.common.DeviceAdminHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BypassProtectionUiState(
    val settingsGuardMode: SettingsGuardMode = SettingsGuardMode.OFF,
    val quarantineHours: QuarantineHours = QuarantineHours.TWENTY_FOUR,
    val uninstallLockEnabled: Boolean = false,
    val isDeviceAdminActive: Boolean = false,
    val quarantinedAppsCount: Int = 0
)

@HiltViewModel
class BypassProtectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository
) : ViewModel() {

    private val _requestEnableAdmin = MutableSharedFlow<Unit>()
    val requestEnableAdmin = _requestEnableAdmin.asSharedFlow()

    val uiState = combine(
        settingsRepository.settingsGuardMode,
        settingsRepository.quarantineHours,
        settingsRepository.uninstallLockEnabled,
        appRuleRepository.observeQuarantinedCount()
    ) { guardMode, qHours, uninstallLock, quarantinedCount ->
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val adminComponent = DeviceAdminHelper.getAdminComponentName(context)
        val isAdminActive = dpm?.isAdminActive(adminComponent) ?: false

        BypassProtectionUiState(
            settingsGuardMode = guardMode,
            quarantineHours = qHours,
            uninstallLockEnabled = uninstallLock,
            isDeviceAdminActive = isAdminActive,
            quarantinedAppsCount = quarantinedCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        BypassProtectionUiState()
    )

    fun updateSettingsGuardMode(mode: SettingsGuardMode) {
        viewModelScope.launch { settingsRepository.setSettingsGuardMode(mode) }
    }

    fun updateQuarantineHours(hours: QuarantineHours) {
        viewModelScope.launch { settingsRepository.setQuarantineHours(hours) }
    }

    fun updateUninstallLock(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                _requestEnableAdmin.emit(Unit)
            } else {
                settingsRepository.setUninstallLockEnabled(false)
            }
        }
    }

    fun onAdminEnabled() {
        viewModelScope.launch { settingsRepository.setUninstallLockEnabled(true) }
    }
}
