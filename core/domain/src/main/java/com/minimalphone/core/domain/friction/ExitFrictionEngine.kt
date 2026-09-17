package com.minimalphone.core.domain.friction

import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.ExitAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.ExitAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.IntentReflectionDefaults
import com.minimalphone.core.model.PendingExitAttempt
import javax.inject.Inject

class CalculateExitFrictionUseCase @Inject constructor() {

    /**
     * Adaptive exit friction progression:
     * 1st attempt (0 prior) -> 5 seconds
     * 2nd attempt (1 prior) -> 30 seconds
     * 3rd attempt (2 prior) -> 120 seconds (2 minutes)
     * 4th+ attempt (3+ prior) -> 600 seconds (10 minutes)
     */
    operator fun invoke(priorAttemptsCount: Int): Int {
        return when (priorAttemptsCount) {
            0 -> 5
            1 -> 30
            2 -> 120
            else -> 600
        }
    }
}

class PrepareExitAttemptUseCase @Inject constructor(
    private val exitAttemptRepository: ExitAttemptRepository,
    private val calculateExitFrictionUseCase: CalculateExitFrictionUseCase
) {
    suspend operator fun invoke(session: FocusSession): PendingExitAttempt {
        val priorAttempts = exitAttemptRepository.getExitAttemptsCount(session.id)
        val attemptNumber = priorAttempts + 1
        val requiredFrictionSeconds = calculateExitFrictionUseCase(priorAttempts)
        val remainingMins = ((session.remainingMillis / 60000).toInt()).coerceAtLeast(1)

        return PendingExitAttempt(
            sessionId = session.id,
            attemptNumber = attemptNumber,
            requiredFrictionSeconds = requiredFrictionSeconds,
            goal = session.goal,
            remainingMinutes = remainingMins
        )
    }
}

class CompleteExitAttemptUseCase @Inject constructor(
    private val exitAttemptRepository: ExitAttemptRepository,
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        attemptNumber: Int,
        requiredFrictionSeconds: Int,
        completedFrictionSeconds: Int,
        exitReason: String? = null
    ) {
        val attempt = ExitAttempt(
            focusSessionId = sessionId,
            attemptNumber = attemptNumber,
            requiredFrictionSeconds = requiredFrictionSeconds,
            completedFrictionSeconds = completedFrictionSeconds,
            timestamp = System.currentTimeMillis(),
            isSuccessfulExit = true,
            exitReason = exitReason
        )
        exitAttemptRepository.recordExitAttempt(attempt)
        focusSessionRepository.cancelSession(sessionId, completedFrictionSeconds, exitReason)
    }
}

class AbortExitAttemptUseCase @Inject constructor(
    private val exitAttemptRepository: ExitAttemptRepository,
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        attemptNumber: Int,
        requiredFrictionSeconds: Int,
        secondsWaited: Int
    ) {
        val attempt = ExitAttempt(
            focusSessionId = sessionId,
            attemptNumber = attemptNumber,
            requiredFrictionSeconds = requiredFrictionSeconds,
            completedFrictionSeconds = secondsWaited,
            timestamp = System.currentTimeMillis(),
            isSuccessfulExit = false,
            exitReason = "User chose to stay in focus"
        )
        exitAttemptRepository.recordExitAttempt(attempt)
        focusSessionRepository.incrementBypassAttempt(sessionId)
    }
}

class RecordIntentReflectionUseCase @Inject constructor(
    private val blockedAttemptRepository: BlockedAttemptRepository
) {
    companion object {
        val INTENT_OPTIONS = listOf(
            "Important / Urgent",
            "Just checking",
            "I'm bored",
            "Someone sent me something",
            "Other"
        )
    }

    suspend operator fun invoke(
        packageName: String,
        appLabel: String,
        focusSessionId: String?,
        userIntentReason: String
    ) {
        blockedAttemptRepository.recordBlockedAttempt(
            BlockedAttempt(
                packageName = packageName,
                appLabel = appLabel,
                timestamp = System.currentTimeMillis(),
                focusSessionId = focusSessionId,
                route = "REFLECTION_PROMPT",
                userIntentReason = userIntentReason
            )
        )
    }
}
