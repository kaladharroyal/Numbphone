package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppRuleRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.pause.PauseDecision
import com.minimalphone.core.domain.pause.PauseEngine
import com.minimalphone.core.domain.quarantine.QuarantineEngine
import com.minimalphone.core.domain.rules.RuleContext
import com.minimalphone.core.domain.rules.RuleEngine
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusMode
import javax.inject.Inject

/**
 * Evaluates whether a given app should be launched, blocked, or shown friction.
 *
 * All policy decisions flow through a strict priority chain:
 *   1. EmergencyAccessManager — system and user-configured essential apps (always allow)
 *   2. QuarantineEngine (M21) — new apps under quarantine hold
 *   3. AppTimeLimitRepository — daily time limit enforcement with emergency extension
 *   4. RuleEngine             — all other rules (focus session, budget M14, schedule M15, dumb mode M16…)
 *
 * This use case is the ONLY entry point for launch decisions.
 */
class EvaluateAppLaunchUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val emergencyAccessManager: EmergencyAccessManager,
    private val ruleEngine: RuleEngine,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val appRuleRepository: AppRuleRepository? = null,
    private val quarantineEngine: QuarantineEngine = QuarantineEngine(),
    private val pauseEngine: PauseEngine? = null
) {
    suspend operator fun invoke(packageName: String): AppLaunchDecision {
        // App must be in DB to be launched
        val app = appRepository.getApp(packageName)
            ?: return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "Application is not installed or launchable.",
                activeSession = null
            )

        // 1. Emergency or Essential apps bypass ALL rules immediately (no pause)
        if (emergencyAccessManager.isEmergencyOrEssential(packageName) || app.isEssential || app.category == AppCategory.ESSENTIAL) {
            return AppLaunchDecision.EmergencyAllow(
                packageName = packageName,
                activityName = app.activityName
            )
        }

        // 2. Daily limit exceeded (M14) -> block
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

        // 3. Focus block (M6) -> block
        val activeSession = focusSessionRepository.getActiveSessionSync()
        if (activeSession != null && activeSession.isCurrentlyActive) {
            if (activeSession.mode == FocusMode.STRICT || activeSession.mode == FocusMode.DEEP_FOCUS) {
                return AppLaunchDecision.Block(
                    packageName = app.packageName,
                    reason = "This app is restricted during your '${activeSession.goal.title}' focus session.",
                    activeSession = activeSession
                )
            }
            if (activeSession.mode == FocusMode.LIGHT) {
                return AppLaunchDecision.ShowFriction(
                    packageName = app.packageName,
                    activeSession = activeSession,
                    frictionSeconds = 5
                )
            }
        }

        // 4. Quarantine (M21) -> block
        val quarantineUntil = appRuleRepository?.getQuarantineUntil(packageName)
        if (quarantineEngine.isQuarantined(quarantineUntil, System.currentTimeMillis())) {
            val remaining = quarantineEngine.remainingHours(quarantineUntil, System.currentTimeMillis())
            return AppLaunchDecision.Block(
                packageName = packageName,
                reason = "App is currently in quarantine (Available in $remaining).",
                activeSession = activeSession
            )
        }

        // 5. Pause required (M22) -> AppLaunchDecision.ShowPause(config, delaySeconds)
        val pauseDecision = pauseEngine?.evaluate(packageName) ?: PauseDecision.NoPause
        if (pauseDecision is PauseDecision.Pause) {
            return AppLaunchDecision.ShowPause(
                packageName = app.packageName,
                activityName = app.activityName,
                config = pauseDecision.config,
                delaySeconds = pauseDecision.delaySeconds
            )
        }

        // 6. Otherwise -> allow (delegate to RuleEngine for budget / dumb mode policies)
        val context = RuleContext(
            app = app,
            activeSession = activeSession
        )
        return ruleEngine.evaluate(context)
    }
}

class RecordBlockedAttemptUseCase @Inject constructor(
    private val blockedAttemptRepository: BlockedAttemptRepository
) {
    suspend operator fun invoke(attempt: BlockedAttempt) {
        blockedAttemptRepository.recordBlockedAttempt(attempt)
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
