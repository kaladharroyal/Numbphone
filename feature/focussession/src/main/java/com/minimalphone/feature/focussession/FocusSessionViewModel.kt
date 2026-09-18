package com.minimalphone.feature.focussession

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.ScheduleRepository
import com.minimalphone.core.domain.focus.CancelFocusSessionUseCase
import com.minimalphone.core.domain.focus.CompleteFocusSessionUseCase
import com.minimalphone.core.domain.focus.FocusTickerState
import com.minimalphone.core.domain.focus.GetActiveFocusSessionUseCase
import com.minimalphone.core.domain.focus.GetFocusGoalsUseCase
import com.minimalphone.core.domain.focus.ObserveFocusSessionTickerUseCase
import com.minimalphone.core.domain.focus.StartFocusSessionUseCase
import com.minimalphone.core.domain.friction.AbortExitAttemptUseCase
import com.minimalphone.core.domain.friction.CompleteExitAttemptUseCase
import com.minimalphone.core.domain.friction.PrepareExitAttemptUseCase
import com.minimalphone.core.domain.schedule.FocusScheduler
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusPreset
import com.minimalphone.core.model.FocusSchedule
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.PendingExitAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class FocusSessionUiState(
    val isSessionActive: Boolean = false,
    val activeSession: FocusSession? = null,
    val ticker: FocusTickerState = FocusTickerState(),
    val selectedDurationMinutes: Int = 25,
    val selectedMode: FocusMode = FocusMode.STRICT,
    val selectedGoal: FocusGoal? = null,
    val customGoalText: String = "",
    val isCustomGoalSelected: Boolean = false,
    val availableGoals: List<FocusGoal> = emptyList(),
    val pendingExitAttempt: PendingExitAttempt? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FocusSessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getFocusGoalsUseCase: GetFocusGoalsUseCase,
    private val getActiveFocusSessionUseCase: GetActiveFocusSessionUseCase,
    private val observeFocusSessionTickerUseCase: ObserveFocusSessionTickerUseCase,
    private val startFocusSessionUseCase: StartFocusSessionUseCase,
    private val completeFocusSessionUseCase: CompleteFocusSessionUseCase,
    private val cancelFocusSessionUseCase: CancelFocusSessionUseCase,
    private val prepareExitAttemptUseCase: PrepareExitAttemptUseCase,
    private val completeExitAttemptUseCase: CompleteExitAttemptUseCase,
    private val abortExitAttemptUseCase: AbortExitAttemptUseCase,
    private val scheduleRepository: ScheduleRepository,
    private val focusScheduler: FocusScheduler,
    private val settingsRepository: com.minimalphone.core.data.repository.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusSessionUiState())
    val uiState: StateFlow<FocusSessionUiState> = _uiState.asStateFlow()

    val presets: StateFlow<List<FocusPreset>> =
        scheduleRepository.observePresets()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val schedules: StateFlow<List<FocusSchedule>> =
        scheduleRepository.observeSchedules()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        loadGoals()
        observeActiveSession()
        observeTicker()
        seedPresetsIfEmpty()
    }

    private fun seedPresetsIfEmpty() {
        viewModelScope.launch {
            scheduleRepository.seedDefaultPresets()
            focusScheduler.evaluateScheduledFocus()
            focusScheduler.scheduleNextAlarm()
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            getFocusGoalsUseCase().collect { goals ->
                val currentSelected = _uiState.value.selectedGoal
                val defaultGoal = currentSelected ?: goals.firstOrNull()
                _uiState.value = _uiState.value.copy(
                    availableGoals = goals,
                    selectedGoal = defaultGoal
                )
            }
        }
    }

    private fun observeActiveSession() {
        viewModelScope.launch {
            getActiveFocusSessionUseCase().collect { session ->
                val isActive = session != null && session.isCurrentlyActive
                _uiState.value = _uiState.value.copy(
                    isSessionActive = isActive,
                    activeSession = session
                )
            }
        }
    }

    private fun observeTicker() {
        viewModelScope.launch {
            observeFocusSessionTickerUseCase().collect { tickerState ->
                _uiState.value = _uiState.value.copy(
                    ticker = tickerState,
                    isSessionActive = tickerState.isSessionActive
                )
            }
        }
    }

    fun onSelectDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(selectedDurationMinutes = minutes)
    }

    fun onSelectMode(mode: FocusMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun onSelectGoal(goal: FocusGoal) {
        _uiState.value = _uiState.value.copy(
            selectedGoal = goal,
            isCustomGoalSelected = false,
            customGoalText = ""
        )
    }

    fun onSelectPreset(preset: FocusPreset) {
        _uiState.value = _uiState.value.copy(
            selectedDurationMinutes = preset.durationMinutes,
            selectedMode = preset.mode,
            selectedGoal = FocusGoal(
                id = preset.id,
                title = preset.title,
                category = preset.category
            ),
            isCustomGoalSelected = false,
            customGoalText = ""
        )
    }

    fun onSelectCustomGoal() {
        _uiState.value = _uiState.value.copy(
            isCustomGoalSelected = true,
            selectedGoal = null
        )
    }

    fun onCustomGoalTextChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            customGoalText = text
        )
    }

    fun onToggleSchedule(scheduleId: String, enabled: Boolean) {
        viewModelScope.launch {
            scheduleRepository.setScheduleEnabled(scheduleId, enabled)
            focusScheduler.scheduleNextAlarm()
        }
    }

    fun onSaveSchedule(schedule: FocusSchedule) {
        viewModelScope.launch {
            scheduleRepository.saveSchedule(schedule)
            focusScheduler.scheduleNextAlarm()
        }
    }

    fun onDeleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(scheduleId)
            focusScheduler.scheduleNextAlarm()
        }
    }

    fun onStartFocusSession() {
        viewModelScope.launch {
            val state = _uiState.value
            val effectiveGoal = if (state.isCustomGoalSelected) {
                if (state.customGoalText.trim().isBlank()) {
                    _uiState.value = _uiState.value.copy(errorMessage = "Please enter a focus goal.")
                    return@launch
                }
                FocusGoal(
                    id = "custom_${UUID.randomUUID()}",
                    title = state.customGoalText.trim(),
                    category = "Custom",
                    isCustom = true
                )
            } else {
                state.selectedGoal ?: FocusGoal(
                    id = "default_study",
                    title = "Study & Exam Prep",
                    category = "Study"
                )
            }

            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = startFocusSessionUseCase(
                goal = effectiveGoal,
                durationMinutes = state.selectedDurationMinutes,
                mode = state.selectedMode
            )

            result.fold(
                onSuccess = { session ->
                    com.minimalphone.core.common.DndHelper.enablePriorityCallsOnlyDnd(context)
                    if (runCatching { settingsRepository.isAutoGrayscaleInFocusEnabled.first() }.getOrDefault(false)) {
                        com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, true)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSessionActive = true,
                        activeSession = session
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to start focus session."
                    )
                }
            )
        }
    }

    fun onRequestExitSession() {
        viewModelScope.launch {
            val session = _uiState.value.activeSession ?: return@launch

            // If session is in LIGHT mode, allow direct completion without friction delay
            if (session.mode == FocusMode.LIGHT) {
                completeFocusSessionUseCase(session.id)
                com.minimalphone.core.common.DndHelper.restoreNormalNotifications(context)
                if (runCatching { settingsRepository.isAutoGrayscaleInFocusEnabled.first() }.getOrDefault(false)) {
                    com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, false)
                }
                return@launch
            }

            // In STRICT or DEEP_FOCUS mode, compute adaptive exit friction
            val pending = prepareExitAttemptUseCase(session)
            _uiState.value = _uiState.value.copy(pendingExitAttempt = pending)
        }
    }

    fun onAbortExitAttempt(secondsWaited: Int) {
        viewModelScope.launch {
            val pending = _uiState.value.pendingExitAttempt ?: return@launch
            abortExitAttemptUseCase(
                sessionId = pending.sessionId,
                attemptNumber = pending.attemptNumber,
                requiredFrictionSeconds = pending.requiredFrictionSeconds,
                secondsWaited = secondsWaited
            )
            _uiState.value = _uiState.value.copy(pendingExitAttempt = null)
        }
    }

    fun onConfirmExitAttempt(completedSeconds: Int, exitReason: String? = null) {
        viewModelScope.launch {
            val pending = _uiState.value.pendingExitAttempt ?: return@launch
            completeExitAttemptUseCase(
                sessionId = pending.sessionId,
                attemptNumber = pending.attemptNumber,
                requiredFrictionSeconds = pending.requiredFrictionSeconds,
                completedFrictionSeconds = completedSeconds,
                exitReason = exitReason
            )
            com.minimalphone.core.common.DndHelper.restoreNormalNotifications(context)
            if (runCatching { settingsRepository.isAutoGrayscaleInFocusEnabled.first() }.getOrDefault(false)) {
                com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, false)
            }
            _uiState.value = _uiState.value.copy(
                pendingExitAttempt = null,
                isSessionActive = false,
                activeSession = null
            )
        }
    }

    fun onEndSessionDirectly() {
        viewModelScope.launch {
            val sessionId = _uiState.value.activeSession?.id ?: return@launch
            completeFocusSessionUseCase(sessionId)
            com.minimalphone.core.common.DndHelper.restoreNormalNotifications(context)
            if (runCatching { settingsRepository.isAutoGrayscaleInFocusEnabled.first() }.getOrDefault(false)) {
                com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, false)
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
