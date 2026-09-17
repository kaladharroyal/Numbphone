package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusMode
import javax.inject.Inject

class EvaluateAppLaunchUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        val app = appRepository.getApp(packageName)
            ?: return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "Application is not installed or launchable.",
                activeSession = null
            )

        // Rule 1: Always Available (Essential) apps are never blocked
        if (app.category == AppCategory.ESSENTIAL || app.isEssential) {
            return AppLaunchDecision.Allow(
                packageName = app.packageName,
                activityName = app.activityName
            )
        }

        // Rule 2: Check if Focus Session is currently active
        val activeSession = focusSessionRepository.getActiveSessionSync()
        if (activeSession == null || !activeSession.isCurrentlyActive) {
            return AppLaunchDecision.Allow(
                packageName = app.packageName,
                activityName = app.activityName
            )
        }

        // Rule 3: Active Focus Session is running for a Managed App
        return when (activeSession.mode) {
            FocusMode.LIGHT -> {
                AppLaunchDecision.ShowFriction(
                    packageName = app.packageName,
                    activeSession = activeSession,
                    frictionSeconds = 5
                )
            }
            FocusMode.STRICT,
            FocusMode.DEEP_FOCUS -> {
                AppLaunchDecision.Block(
                    packageName = app.packageName,
                    reason = "This app is restricted during your '${activeSession.goal.title}' focus session.",
                    activeSession = activeSession
                )
            }
        }
    }
}

class LaunchAppUseCase @Inject constructor(
    private val evaluateAppLaunchUseCase: EvaluateAppLaunchUseCase,
    private val appRepository: AppRepository,
    private val blockedAttemptRepository: BlockedAttemptRepository
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        val decision = evaluateAppLaunchUseCase(packageName)

        if (decision is AppLaunchDecision.Block) {
            val app = appRepository.getApp(packageName)
            val label = app?.label ?: packageName.substringAfterLast('.')

            blockedAttemptRepository.recordBlockedAttempt(
                BlockedAttempt(
                    packageName = packageName,
                    appLabel = label,
                    timestamp = System.currentTimeMillis(),
                    focusSessionId = decision.activeSession?.id,
                    route = "LAUNCHER",
                    userIntentReason = null
                )
            )
        }

        return decision
    }
}

class RecordBlockedAttemptUseCase @Inject constructor(
    private val blockedAttemptRepository: BlockedAttemptRepository
) {
    suspend operator fun invoke(attempt: BlockedAttempt) {
        blockedAttemptRepository.recordBlockedAttempt(attempt)
    }
}
