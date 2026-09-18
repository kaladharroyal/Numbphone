package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.rules.RuleContext
import com.minimalphone.core.domain.rules.RuleEngine
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import javax.inject.Inject

/**
 * Evaluates whether a given app should be launched, blocked, or shown friction.
 *
 * All policy decisions flow through a strict priority chain:
 *   1. EmergencyAccessManager — system and user-configured essential apps (always allow)
 *   2. AppTimeLimitRepository — daily time limit enforcement with emergency extension
 *   3. RuleEngine             — all other rules (focus session, budget M14, schedule M15, dumb mode M16…)
 *
 * This use case is the ONLY entry point for launch decisions.
 */
class EvaluateAppLaunchUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val emergencyAccessManager: EmergencyAccessManager,
    private val ruleEngine: RuleEngine,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        // App must be in DB to be launched
        val app = appRepository.getApp(packageName)
            ?: return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "Application is not installed or launchable.",
                activeSession = null
            )

        // Check Per-App Daily Time Limits (Applies to all configured apps)
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

        // Emergency / Essential apps bypass Focus Sessions and Dumb Mode
        if (emergencyAccessManager.isEmergencyOrEssential(packageName)) {
            return AppLaunchDecision.EmergencyAllow(
                packageName = packageName,
                activityName = app.activityName
            )
        }

        // Delegate to RuleEngine (focus, budget M14, schedule M15, dumb mode M16…)
        val activeSession = focusSessionRepository.getActiveSessionSync()
        val context = RuleContext(
            app = app,
            activeSession = activeSession
        )
        return ruleEngine.evaluate(context)
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
