package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusMode
import javax.inject.Inject

class EvaluateAppLaunchUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        val app = appRepository.getApp(packageName)
            ?: return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "Application is not installed or launchable.",
                activeSession = null
            )

        // Rule 1: Check Per-App Daily Time Limits (Applies to all managed and configured apps)
        val timeLimit = appTimeLimitRepository.getLimit(packageName)
        if (timeLimit != null && timeLimit.isEnabled && timeLimit.effectiveDailyLimitMinutes > 0) {
            val usedMinutes = usageStatsRepository.getTodayUsageMinutes(packageName)
            if (usedMinutes >= timeLimit.effectiveDailyLimitMinutes) {
                val limitStr = timeLimit.formattedEffectiveLimit
                return AppLaunchDecision.Block(
                    packageName = app.packageName,
                    reason = "Daily limit of $limitStr reached for today (${usedMinutes}m used).",
                    activeSession = null
                )
            }
        }

        // Rule 2: Always Available (Essential) apps are never blocked by Focus Sessions
        if (app.category == AppCategory.ESSENTIAL || app.isEssential) {
            return AppLaunchDecision.Allow(
                packageName = app.packageName,
                activityName = app.activityName
            )
        }

        // Rule 3: Check if Focus Session is currently active
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
