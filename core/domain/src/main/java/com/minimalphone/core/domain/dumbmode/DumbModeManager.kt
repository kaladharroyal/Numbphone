package com.minimalphone.core.domain.dumbmode

import com.minimalphone.core.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Dumb Phone (Extreme Detox) Mode state.
 * When Dumb Mode is enabled, the launcher policy restricts the home screen
 * to essential communication apps only, disabling managed apps and distractions.
 */
@Singleton
class DumbModeManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    val isDumbModeEnabled: Flow<Boolean> = settingsRepository.isDumbModeEnabled

    suspend fun setDumbModeEnabled(enabled: Boolean) {
        settingsRepository.setDumbModeEnabled(enabled)
    }

    suspend fun toggleDumbMode(current: Boolean) {
        settingsRepository.setDumbModeEnabled(!current)
    }
}
