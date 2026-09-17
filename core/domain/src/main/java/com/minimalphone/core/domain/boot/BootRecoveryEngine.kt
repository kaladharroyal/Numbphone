package com.minimalphone.core.domain.boot

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

sealed interface BootRecoveryResult {
    data class Restored(val session: FocusSession) : BootRecoveryResult
    data class ExpiredAndCompleted(val session: FocusSession) : BootRecoveryResult
    data object NoActiveSession : BootRecoveryResult
}

class RestoreFocusOnBootUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository,
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): BootRecoveryResult {
        // 1. Resync installed applications to capture offline updates
        try {
            appRepository.syncInstalledApps()
        } catch (_: Exception) {
            // Non-critical if package sync fails on early boot
        }

        // 2. Query active session
        val activeSession = focusSessionRepository.getActiveSessionSync()
            ?: return BootRecoveryResult.NoActiveSession

        val currentTime = System.currentTimeMillis()
        return if (currentTime < activeSession.endTime) {
            BootRecoveryResult.Restored(activeSession)
        } else {
            // Session expired while device was powered off
            focusSessionRepository.completeSession(activeSession.id)
            BootRecoveryResult.ExpiredAndCompleted(activeSession)
        }
    }
}

data class SystemDiagnosticsData(
    val isDefaultLauncher: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isUsageAccessGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val activeSession: FocusSession? = null,
    val todayBlockedCount: Int = 0,
    val oemName: String = "",
    val deviceModel: String = "",
    val androidVersion: Int = 0,
    val oemGuidance: String? = null
)

class ObserveSystemDiagnosticsUseCase @Inject constructor(
    private val focusSessionRepository: FocusSessionRepository,
    private val blockedAttemptRepository: BlockedAttemptRepository
) {
    operator fun invoke(
        isDefaultLauncher: Boolean,
        isAccessibilityEnabled: Boolean,
        isUsageAccessGranted: Boolean,
        isBatteryOptimizationIgnored: Boolean,
        oemName: String,
        deviceModel: String,
        androidVersion: Int,
        oemGuidance: String?
    ): Flow<SystemDiagnosticsData> {
        return combine(
            focusSessionRepository.getActiveSession(),
            blockedAttemptRepository.getTodayBlockedCount()
        ) { activeSession, blockedCount ->
            SystemDiagnosticsData(
                isDefaultLauncher = isDefaultLauncher,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isUsageAccessGranted = isUsageAccessGranted,
                isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                activeSession = activeSession,
                todayBlockedCount = blockedCount,
                oemName = oemName,
                deviceModel = deviceModel,
                androidVersion = androidVersion,
                oemGuidance = oemGuidance
            )
        }
    }
}
