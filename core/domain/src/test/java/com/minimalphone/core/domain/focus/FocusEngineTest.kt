package com.minimalphone.core.domain.focus

import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeFocusSessionRepository : FocusSessionRepository {
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

class FocusEngineTest {

    private lateinit var repository: FakeFocusSessionRepository
    private lateinit var startFocusSessionUseCase: StartFocusSessionUseCase
    private lateinit var getActiveFocusSessionUseCase: GetActiveFocusSessionUseCase
    private lateinit var completeFocusSessionUseCase: CompleteFocusSessionUseCase
    private lateinit var cancelFocusSessionUseCase: CancelFocusSessionUseCase
    private lateinit var getFocusGoalsUseCase: GetFocusGoalsUseCase
    private lateinit var observeFocusSessionTickerUseCase: ObserveFocusSessionTickerUseCase

    @Before
    fun setUp() {
        repository = FakeFocusSessionRepository()
        startFocusSessionUseCase = StartFocusSessionUseCase(repository)
        getActiveFocusSessionUseCase = GetActiveFocusSessionUseCase(repository)
        completeFocusSessionUseCase = CompleteFocusSessionUseCase(repository)
        cancelFocusSessionUseCase = CancelFocusSessionUseCase(repository)
        getFocusGoalsUseCase = GetFocusGoalsUseCase(repository)
        observeFocusSessionTickerUseCase = ObserveFocusSessionTickerUseCase(repository)
    }

    @Test
    fun startFocusSession_createsValidActiveSession() = runTest {
        val goal = FocusGoal(id = "1", title = "DBMS Revision", category = "Study")
        val result = startFocusSessionUseCase(
            goal = goal,
            durationMinutes = 45,
            mode = FocusMode.STRICT
        )

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("DBMS Revision", session?.goal?.title)
        assertEquals(45, session?.durationMinutes)
        assertEquals(FocusMode.STRICT, session?.mode)
        assertEquals(SessionStatus.ACTIVE, session?.status)
        assertTrue(session?.isCurrentlyActive == true)

        val active = getActiveFocusSessionUseCase().first()
        assertEquals(session?.id, active?.id)
    }

    @Test
    fun startFocusSession_rejectsBlankGoal() = runTest {
        val goal = FocusGoal(id = "2", title = "   ", category = "Study")
        val result = startFocusSessionUseCase(goal = goal, durationMinutes = 30)

        assertTrue(result.isFailure)
    }

    @Test
    fun completeFocusSession_clearsActiveSessionAndUpdatesStatus() = runTest {
        val goal = FocusGoal(id = "1", title = "Algorithms", category = "Study")
        val session = startFocusSessionUseCase(goal, 25).getOrThrow()

        completeFocusSessionUseCase(session.id)

        val active = getActiveFocusSessionUseCase().first()
        assertEquals(null, active)
        val all = repository.allSessionsFlow.value
        assertEquals(SessionStatus.COMPLETED, all.first().status)
    }

    @Test
    fun cancelFocusSession_recordsFrictionSecondsAndUpdatesStatus() = runTest {
        val goal = FocusGoal(id = "1", title = "System Design", category = "Work")
        val session = startFocusSessionUseCase(goal, 60).getOrThrow()

        cancelFocusSessionUseCase(session.id, frictionSeconds = 30, reason = "Emergency call")

        val active = getActiveFocusSessionUseCase().first()
        assertEquals(null, active)
        val all = repository.allSessionsFlow.value
        assertEquals(SessionStatus.CANCELLED_WITH_FRICTION, all.first().status)
        assertEquals(30, all.first().exitFrictionSecondsCompleted)
    }

    @Test
    fun getFocusGoals_includesDefaultPresetsAndCustomRecents() = runTest {
        repository.recentGoalsFlow.value = listOf(
            FocusGoal(id = "custom_1", title = "Calculus Assignment", category = "Study", isCustom = true)
        )

        val goals = getFocusGoalsUseCase().first()
        assertTrue(goals.any { it.title == "Finish DBMS Revision" })
        assertTrue(goals.any { it.title == "Calculus Assignment" })
    }

    @Test
    fun observeTicker_emitsInactiveWhenNoSession() = runTest {
        val ticker = observeFocusSessionTickerUseCase().first()
        assertFalse(ticker.isSessionActive)
        assertEquals("00:00", ticker.formattedTime)
    }
}
