package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.rules.RuleContext
import com.minimalphone.core.domain.rules.RuleEngine
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import javax.inject.Inject

/**
 * Evaluates whether a given app should be launched, blocked, or shown friction.
 *
 * All policy decisions flow through a strict priority chain:
 *   1. EmergencyAccessManager — system and user-configured essential apps (always allow)
 *   2. RuleEngine             — all other rules (focus session, budget M14, schedule M15, dumb mode M16…)
 *
 * This use case is the ONLY entry point for launch decisions.
 * Adding new rules requires only a change to RuleEngine, never to this class.
 */
class EvaluateAppLaunchUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val emergencyAccessManager: EmergencyAccessManager,
    private val ruleEngine: RuleEngine
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        // Priority 0: Emergency / Essential apps bypass ALL rules immediately
        if (emergencyAccessManager.isEmergencyOrEssential(packageName)) {
            val app = appRepository.getApp(packageName)
            return AppLaunchDecision.EmergencyAllow(
                packageName = packageName,
                activityName = app?.activityName ?: ""
            )
        }

        // App must be in DB to be launched
        val app = appRepository.getApp(packageName)
            ?: return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "Application is not installed or launchable.",
                activeSession = null
            )

        // Priority 1+: delegate to RuleEngine (focus, budget M14, schedule M15, dumb mode M16…)
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
