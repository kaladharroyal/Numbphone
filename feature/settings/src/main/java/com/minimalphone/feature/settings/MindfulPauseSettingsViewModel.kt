package com.minimalphone.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.PauseStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MindfulPauseUiState(
    val globalEnabled: Boolean = true,
    val pauseSeconds: Int = 5,
    val sessionMinutes: Int = 10,
    val pauseStyle: PauseStyle = PauseStyle.BREATHE,
    val progressiveDelayEnabled: Boolean = false
)

@HiltViewModel
class MindfulPauseSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState = combine(
        settingsRepository.pauseGlobalEnabled,
        settingsRepository.defaultPauseSeconds,
        settingsRepository.defaultSessionMinutes,
        settingsRepository.defaultPauseStyle,
        settingsRepository.progressiveDelayEnabled
    ) { global, seconds, minutes, style, progressive ->
        MindfulPauseUiState(
            globalEnabled = global,
            pauseSeconds = seconds,
            sessionMinutes = minutes,
            pauseStyle = style,
            progressiveDelayEnabled = progressive
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MindfulPauseUiState()
    )

    fun setGlobalEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPauseGlobalEnabled(enabled) }
    }

    fun setPauseSeconds(seconds: Int) {
        viewModelScope.launch { settingsRepository.setDefaultPauseSeconds(seconds) }
    }

    fun setSessionMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.setDefaultSessionMinutes(minutes) }
    }

    fun setPauseStyle(style: PauseStyle) {
        viewModelScope.launch { settingsRepository.setDefaultPauseStyle(style) }
    }

    fun setProgressiveDelayEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setProgressiveDelayEnabled(enabled) }
    }
}
