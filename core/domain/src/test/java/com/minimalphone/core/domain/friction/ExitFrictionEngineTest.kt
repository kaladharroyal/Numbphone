package com.minimalphone.core.domain.friction

import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.ExitAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.BlockedAttempt
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeTestExitAttemptRepository : ExitAttemptRepository {
    val recordedAttempts = mutableListOf<ExitAttempt>()

    override fun getExitAttemptsForSession(sessionId: String): Flow<List<ExitAttempt>> =
        MutableStateFlow(recordedAttempts.filter { it.focusSessionId == sessionId })

    override suspend fun getExitAttemptsCount(sessionId: String): Int =
        recordedAttempts.count { it.focusSessionId == sessionId }

    override suspend fun recordExitAttempt(attempt: ExitAttempt) {
        recordedAttempts.add(attempt)
    }
}

class FakeFrictionFocusSessionRepository : FocusSessionRepository {
    var cancelledSessionId: String? = null
    var cancelledFrictionSeconds: Int? = null
    var bypassCount = 0

    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(null)
    override suspend fun getActiveSessionSync(): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun startSession(session: FocusSession) {}
    override suspend fun completeSession(sessionId: String) {}

    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {
        cancelledSessionId = sessionId
        cancelledFrictionSeconds = exitFrictionSeconds
    }

    override suspend fun incrementBypassAttempt(sessionId: String) {
        bypassCount++
    }

    override fun getRecentGoals(): Flow<List<FocusGoal>> = MutableStateFlow(emptyList())
}

class FakeTestBlockedAttemptRepository : BlockedAttemptRepository {
    val recorded = mutableListOf<BlockedAttempt>()

    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = MutableStateFlow(recorded)
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(recorded.size)

    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {
        recorded.add(attempt)
    }
}

class ExitFrictionEngineTest {

    private lateinit var calculateExitFrictionUseCase: CalculateExitFrictionUseCase
    private lateinit var exitAttemptRepository: FakeTestExitAttemptRepository
    private lateinit var focusSessionRepository: FakeFrictionFocusSessionRepository
    private lateinit var blockedAttemptRepository: FakeTestBlockedAttemptRepository
    private lateinit var prepareExitAttemptUseCase: PrepareExitAttemptUseCase
    private lateinit var completeExitAttemptUseCase: CompleteExitAttemptUseCase
    private lateinit var abortExitAttemptUseCase: AbortExitAttemptUseCase
    private lateinit var recordIntentReflectionUseCase: RecordIntentReflectionUseCase

    @Before
    fun setUp() {
        calculateExitFrictionUseCase = CalculateExitFrictionUseCase()
        exitAttemptRepository = FakeTestExitAttemptRepository()
        focusSessionRepository = FakeFrictionFocusSessionRepository()
        blockedAttemptRepository = FakeTestBlockedAttemptRepository()

        prepareExitAttemptUseCase = PrepareExitAttemptUseCase(exitAttemptRepository, calculateExitFrictionUseCase)
        completeExitAttemptUseCase = CompleteExitAttemptUseCase(exitAttemptRepository, focusSessionRepository)
        abortExitAttemptUseCase = AbortExitAttemptUseCase(exitAttemptRepository, focusSessionRepository)
        recordIntentReflectionUseCase = RecordIntentReflectionUseCase(blockedAttemptRepository)
    }

    @Test
    fun calculateExitFriction_followsProgression() {
        // 1st attempt -> 5s
        assertEquals(5, calculateExitFrictionUseCase(0))
        // 2nd attempt -> 30s
        assertEquals(30, calculateExitFrictionUseCase(1))
        // 3rd attempt -> 120s (2m)
        assertEquals(120, calculateExitFrictionUseCase(2))
        // 4th attempt -> 600s (10m)
        assertEquals(600, calculateExitFrictionUseCase(3))
        // 5th attempt -> 600s
        assertEquals(600, calculateExitFrictionUseCase(4))
    }

    @Test
    fun prepareExitAttempt_calculatesAttemptNumberAndRequiredFriction() = runTest {
        val session = FocusSession(
            id = "sess_100",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 1800000, // 30 mins remaining
            durationMinutes = 30,
            goal = FocusGoal(id = "g1", title = "Prepare for ML examination"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )

        // First preparation
        val pending1 = prepareExitAttemptUseCase(session)
        assertEquals(1, pending1.attemptNumber)
        assertEquals(5, pending1.requiredFrictionSeconds)
        assertEquals("Prepare for ML examination", pending1.goal.title)

        // Simulate 1 recorded attempt in repo
        exitAttemptRepository.recordExitAttempt(
            ExitAttempt(
                focusSessionId = "sess_100",
                attemptNumber = 1,
                requiredFrictionSeconds = 5,
                completedFrictionSeconds = 5,
                isSuccessfulExit = false
            )
        )

        // Second preparation
        val pending2 = prepareExitAttemptUseCase(session)
        assertEquals(2, pending2.attemptNumber)
        assertEquals(30, pending2.requiredFrictionSeconds)
    }

    @Test
    fun completeExitAttempt_recordsSuccessAndCancelsSession() = runTest {
        completeExitAttemptUseCase(
            sessionId = "sess_100",
            attemptNumber = 1,
            requiredFrictionSeconds = 5,
            completedFrictionSeconds = 5,
            exitReason = "Emergency call"
        )

        assertEquals("sess_100", focusSessionRepository.cancelledSessionId)
        assertEquals(5, focusSessionRepository.cancelledFrictionSeconds)
        assertEquals(1, exitAttemptRepository.recordedAttempts.size)
        assertTrue(exitAttemptRepository.recordedAttempts.first().isSuccessfulExit)
        assertEquals("Emergency call", exitAttemptRepository.recordedAttempts.first().exitReason)
    }

    @Test
    fun abortExitAttempt_recordsUnsuccessfulAndIncrementsBypass() = runTest {
        abortExitAttemptUseCase(
            sessionId = "sess_100",
            attemptNumber = 2,
            requiredFrictionSeconds = 30,
            secondsWaited = 12
        )

        assertEquals(1, focusSessionRepository.bypassCount)
        assertEquals(1, exitAttemptRepository.recordedAttempts.size)
        val attempt = exitAttemptRepository.recordedAttempts.first()
        assertFalse(attempt.isSuccessfulExit)
        assertEquals(12, attempt.completedFrictionSeconds)
    }

    @Test
    fun recordIntentReflection_savesSurveySelection() = runTest {
        recordIntentReflectionUseCase(
            packageName = "com.google.android.youtube",
            appLabel = "YouTube",
            focusSessionId = "sess_100",
            userIntentReason = "I'm bored"
        )

        assertEquals(1, blockedAttemptRepository.recorded.size)
        val record = blockedAttemptRepository.recorded.first()
        assertEquals("YouTube", record.appLabel)
        assertEquals("I'm bored", record.userIntentReason)
        assertEquals("REFLECTION_PROMPT", record.route)
    }
}
