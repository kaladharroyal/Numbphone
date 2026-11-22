package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class UserSettings(
    val isOnboardingCompleted: Boolean = false,
    val isAdaptiveFrictionEnabled: Boolean = true,
    val defaultFocusDurationMinutes: Int = 25,
    val isLocalAnalyticsEnabled: Boolean = true,
    val appTheme: AppTheme = AppTheme.PURE_BLACK,
    val isDumbModeEnabled: Boolean = false,
    val isAutoGrayscaleInFocusEnabled: Boolean = false
)

class GetSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> {
        return combine(
            settingsRepository.isOnboardingCompleted,
            settingsRepository.isAdaptiveFrictionEnabled,
            settingsRepository.defaultFocusDurationMinutes,
            settingsRepository.isLocalAnalyticsEnabled,
            combine(
                settingsRepository.appTheme,
                settingsRepository.isDumbModeEnabled,
                settingsRepository.isAutoGrayscaleInFocusEnabled
            ) { theme, dumbMode, autoGrayscale -> Triple(theme, dumbMode, autoGrayscale) }
        ) { onboarding, adaptive, duration, analytics, triple ->
            UserSettings(
                isOnboardingCompleted = onboarding,
                isAdaptiveFrictionEnabled = adaptive,
                defaultFocusDurationMinutes = duration,
                isLocalAnalyticsEnabled = analytics,
                appTheme = triple.first,
                isDumbModeEnabled = triple.second,
                isAutoGrayscaleInFocusEnabled = triple.third
            )
        }
    }
}
