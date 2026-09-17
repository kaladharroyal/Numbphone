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
    val appTheme: AppTheme = AppTheme.PURE_BLACK
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
            settingsRepository.appTheme
        ) { onboarding, adaptive, duration, analytics, theme ->
            UserSettings(
                isOnboardingCompleted = onboarding,
                isAdaptiveFrictionEnabled = adaptive,
                defaultFocusDurationMinutes = duration,
                isLocalAnalyticsEnabled = analytics,
                appTheme = theme
            )
        }
    }
}

class SetOnboardingCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(completed: Boolean = true) {
        settingsRepository.setOnboardingCompleted(completed)
    }
}

class UpdateAdaptiveFrictionUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setAdaptiveFrictionEnabled(enabled)
    }
}

class UpdateDefaultFocusDurationUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(minutes: Int) {
        settingsRepository.setDefaultFocusDurationMinutes(minutes)
    }
}

class UpdateLocalAnalyticsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        settingsRepository.setLocalAnalyticsEnabled(enabled)
    }
}

class UpdateAppThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(theme: AppTheme) {
        settingsRepository.setAppTheme(theme)
    }
}
