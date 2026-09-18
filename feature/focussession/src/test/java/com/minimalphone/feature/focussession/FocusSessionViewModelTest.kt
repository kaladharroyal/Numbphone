package com.minimalphone.feature.focussession

import com.minimalphone.core.data.repository.ExitAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.domain.focus.CancelFocusSessionUseCase
import com.minimalphone.core.domain.focus.CompleteFocusSessionUseCase
import com.minimalphone.core.domain.focus.GetActiveFocusSessionUseCase
import com.minimalphone.core.domain.focus.GetFocusGoalsUseCase
import com.minimalphone.core.domain.focus.ObserveFocusSessionTickerUseCase
import com.minimalphone.core.domain.focus.StartFocusSessionUseCase
import com.minimalphone.core.domain.friction.AbortExitAttemptUseCase
import com.minimalphone.core.domain.friction.CalculateExitFrictionUseCase
import com.minimalphone.core.domain.friction.CompleteExitAttemptUseCase
import com.minimalphone.core.domain.friction.PrepareExitAttemptUseCase
import com.minimalphone.core.model.ExitAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FakeTestFocusSessionRepository : FocusSessionRepository {
    val activeSessionFlow = MutableStateFlow<FocusSession?>(null)
    val allSessionsFlow = MutableStateFlow<List<FocusSession>>(emptyList())
    val recentGoalsFlow = MutableStateFlow<List<FocusGoal>>(emptyList())

    override fun getActiveSession(): Flow<FocusSession?> = activeSessionFlow
    override suspend fun getActiveSessionSync(): FocusSession? = activeSessionFlow.value
    override fun getAllSessions(): Flow<List<FocusSession>> = allSessionsFlow

    override suspend fun startSession(session: FocusSession) {
        activeSessionFlow.value = session
        allSessionsFlow.value = allSessionsFlow.value + session
    }

    override suspend fun completeSession(sessionId: String) {
        val current = activeSessionFlow.value
        if (current?.id == sessionId) {
            val updated = current.copy(status = SessionStatus.COMPLETED)
            activeSessionFlow.value = null
            allSessionsFlow.value = allSessionsFlow.value.map { if (it.id == sessionId) updated else it }
        }
    }

    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {
        val current = activeSessionFlow.value
        if (current?.id == sessionId) {
            val updated = current.copy(
                status = SessionStatus.CANCELLED_WITH_FRICTION,
                exitFrictionSecondsCompleted = exitFrictionSeconds
            )
            activeSessionFlow.value = null
            allSessionsFlow.value = allSessionsFlow.value.map { if (it.id == sessionId) updated else it }
        }
    }

    override suspend fun incrementBypassAttempt(sessionId: String) {
        val current = activeSessionFlow.value
        if (current?.id == sessionId) {
            activeSessionFlow.value = current.copy(bypassAttemptsCount = current.bypassAttemptsCount + 1)
        }
    }

    override fun getRecentGoals(): Flow<List<FocusGoal>> = recentGoalsFlow
}

class FakeTestExitAttemptRepo : ExitAttemptRepository {
    val recorded = mutableListOf<ExitAttempt>()
    override fun getExitAttemptsForSession(sessionId: String): Flow<List<ExitAttempt>> =
        MutableStateFlow(recorded.filter { it.focusSessionId == sessionId })
    override suspend fun getExitAttemptsCount(sessionId: String): Int =
        recorded.count { it.focusSessionId == sessionId }
    override suspend fun recordExitAttempt(attempt: ExitAttempt) {
        recorded.add(attempt)
    }
}

class FocusSessionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeTestFocusSessionRepository
    private lateinit var exitRepo: FakeTestExitAttemptRepo
    private lateinit var viewModel: FocusSessionViewModel

    @Before
    fun setUp() {
        repository = FakeTestFocusSessionRepository()
        exitRepo = FakeTestExitAttemptRepo()
        val getFocusGoalsUseCase = GetFocusGoalsUseCase(repository)
        val getActiveFocusSessionUseCase = GetActiveFocusSessionUseCase(repository)
        val observeFocusSessionTickerUseCase = ObserveFocusSessionTickerUseCase(repository)
        val startFocusSessionUseCase = StartFocusSessionUseCase(repository)
        val completeFocusSessionUseCase = CompleteFocusSessionUseCase(repository)
        val cancelFocusSessionUseCase = CancelFocusSessionUseCase(repository)
        val calculateExitFrictionUseCase = CalculateExitFrictionUseCase()
        val prepareExitAttemptUseCase = PrepareExitAttemptUseCase(exitRepo, calculateExitFrictionUseCase)
        val completeExitAttemptUseCase = CompleteExitAttemptUseCase(exitRepo, repository)
        val abortExitAttemptUseCase = AbortExitAttemptUseCase(exitRepo, repository)

        viewModel = FocusSessionViewModel(
            context = io.mockk.mockk(relaxed = true),
            getFocusGoalsUseCase = getFocusGoalsUseCase,
            getActiveFocusSessionUseCase = getActiveFocusSessionUseCase,
            observeFocusSessionTickerUseCase = observeFocusSessionTickerUseCase,
            startFocusSessionUseCase = startFocusSessionUseCase,
            completeFocusSessionUseCase = completeFocusSessionUseCase,
            cancelFocusSessionUseCase = cancelFocusSessionUseCase,
            prepareExitAttemptUseCase = prepareExitAttemptUseCase,
            completeExitAttemptUseCase = completeExitAttemptUseCase,
            abortExitAttemptUseCase = abortExitAttemptUseCase,
            scheduleRepository = io.mockk.mockk(relaxed = true),
            focusScheduler = io.mockk.mockk(relaxed = true),
            settingsRepository = io.mockk.mockk(relaxed = true)
        )
    }

    @Test
    fun initialState_loadsGoalsAndSetsDefaults() {
        val state = viewModel.uiState.value
        assertFalse(state.isSessionActive)
        assertEquals(25, state.selectedDurationMinutes)
        assertEquals(FocusMode.STRICT, state.selectedMode)
        assertNotNull(state.selectedGoal)
        assertTrue(state.availableGoals.isNotEmpty())
    }

    @Test
    fun onSelectDuration_updatesSelectedDuration() {
        viewModel.onSelectDuration(45)
        assertEquals(45, viewModel.uiState.value.selectedDurationMinutes)
    }

    @Test
    fun onSelectMode_updatesSelectedMode() {
        viewModel.onSelectMode(FocusMode.DEEP_FOCUS)
        assertEquals(FocusMode.DEEP_FOCUS, viewModel.uiState.value.selectedMode)
    }

    @Test
    fun onSelectCustomGoal_enablesCustomGoalInput() {
        viewModel.onSelectCustomGoal()
        val state = viewModel.uiState.value
        assertTrue(state.isCustomGoalSelected)
        assertEquals(null, state.selectedGoal)

        viewModel.onCustomGoalTextChanged("Complete Compiler Assignment")
        assertEquals("Complete Compiler Assignment", viewModel.uiState.value.customGoalText)
    }

    @Test
    fun onStartFocusSession_withPresetGoal_startsActiveSession() = runTest {
        val goal = viewModel.uiState.value.availableGoals.first { it.title == "Finish DBMS Revision" }
        viewModel.onSelectGoal(goal)
        viewModel.onSelectDuration(60)
        viewModel.onSelectMode(FocusMode.STRICT)

        viewModel.onStartFocusSession()

        val state = viewModel.uiState.value
        assertTrue(state.isSessionActive)
        assertNotNull(state.activeSession)
        assertEquals("Finish DBMS Revision", state.activeSession?.goal?.title)
        assertEquals(60, state.activeSession?.durationMinutes)
        assertEquals(FocusMode.STRICT, state.activeSession?.mode)
    }

    @Test
    fun onStartFocusSession_withEmptyCustomGoal_showsError() = runTest {
        viewModel.onSelectCustomGoal()
        viewModel.onCustomGoalTextChanged("   ")

        viewModel.onStartFocusSession()

        val state = viewModel.uiState.value
        assertFalse(state.isSessionActive)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun onRequestExitSession_inStrictMode_opensExitFrictionDialog() = runTest {
        val goal = viewModel.uiState.value.availableGoals.first()
        viewModel.onSelectGoal(goal)
        viewModel.onSelectMode(FocusMode.STRICT)
        viewModel.onStartFocusSession()

        viewModel.onRequestExitSession()

        val state = viewModel.uiState.value
        assertNotNull(state.pendingExitAttempt)
        assertEquals(1, state.pendingExitAttempt?.attemptNumber)
        assertEquals(5, state.pendingExitAttempt?.requiredFrictionSeconds)
    }

    @Test
    fun onAbortExitAttempt_recordsUnsuccessfulAndClearsPending() = runTest {
        val goal = viewModel.uiState.value.availableGoals.first()
        viewModel.onSelectGoal(goal)
        viewModel.onStartFocusSession()
        viewModel.onRequestExitSession()

        viewModel.onAbortExitAttempt(secondsWaited = 3)

        assertNull(viewModel.uiState.value.pendingExitAttempt)
        assertEquals(1, exitRepo.recorded.size)
        assertFalse(exitRepo.recorded.first().isSuccessfulExit)
        assertEquals(3, exitRepo.recorded.first().completedFrictionSeconds)
    }

    @Test
    fun onConfirmExitAttempt_completesExitAndCancelsSession() = runTest {
        val goal = viewModel.uiState.value.availableGoals.first()
        viewModel.onSelectGoal(goal)
        viewModel.onStartFocusSession()
        viewModel.onRequestExitSession()

        viewModel.onConfirmExitAttempt(completedSeconds = 5, exitReason = "Urgent phone call")

        assertNull(viewModel.uiState.value.pendingExitAttempt)
        assertFalse(viewModel.uiState.value.isSessionActive)
        assertEquals(1, exitRepo.recorded.size)
        assertTrue(exitRepo.recorded.first().isSuccessfulExit)
    }
}
