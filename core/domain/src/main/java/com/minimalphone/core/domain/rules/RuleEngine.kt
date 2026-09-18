package com.minimalphone.core.domain.rules

import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Context passed to the RuleEngine for a launch evaluation.
 */
data class RuleContext(
    val app: InstalledApp,
    val activeSession: FocusSession?,
    val currentTime: LocalTime = LocalTime.now(),
    val currentDay: DayOfWeek = DayOfWeek.from(LocalDate.now())
)

/**
 * Central Rule Engine — the single authority for all launch policy decisions.
 *
 * Priority chain (highest → lowest):
 *  0. Emergency/Essential  → always handled BEFORE RuleEngine (EmergencyAccessManager)
 *  1. DB-Essential flag    → EmergencyAllow
 *  2. Dumb Mode active (M16) → Block if not an essential app
 *  3. Focus session active (M13/M15) → Block or ShowFriction (by mode)
 *  4. Daily time limit exhausted → Block
 *  5. Allow
 */
@Singleton
class RuleEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) {

    suspend fun evaluate(context: RuleContext): AppLaunchDecision {
        val app = context.app
        val session = context.activeSession

        // Step 1: DB-Essential flag
        if (app.isEssential) {
            return AppLaunchDecision.EmergencyAllow(
                packageName = app.packageName,
                activityName = app.activityName
            )
        }

        // Step 2: Dumb Mode check (M16)
        val isDumbMode = runCatching { settingsRepository.isDumbModeEnabled.first() }.getOrDefault(false)
        if (isDumbMode) {
            val isCoreApp = app.label.equals("Phone", ignoreCase = true) ||
                    app.label.equals("Messages", ignoreCase = true) ||
                    app.label.equals("Dialer", ignoreCase = true) ||
                    app.label.equals("Settings", ignoreCase = true)

            if (!isCoreApp) {
                return AppLaunchDecision.Block(
                    packageName = app.packageName,
                    reason = "Dumb Phone Detox Mode is active. Only essential communication apps are allowed.",
                    activeSession = null
                )
            }
        }

        // Step 3: No active focus session
        if (session == null || !session.isCurrentlyActive) {
            return evaluateTimeLimit(app) ?: AppLaunchDecision.Allow(
                packageName = app.packageName,
                activityName = app.activityName
            )
        }

        // Step 3b: Active focus session — apply mode policy
        val focusDecision = when (session.mode) {
            FocusMode.LIGHT -> AppLaunchDecision.ShowFriction(
                packageName = app.packageName,
                activeSession = session,
                frictionSeconds = 5
            )
            FocusMode.STRICT,
            FocusMode.DEEP_FOCUS -> AppLaunchDecision.Block(
                packageName = app.packageName,
                reason = "This app is restricted during your '${session.goal.title}' focus session.",
                activeSession = session
            )
        }

        // If focus already blocks, no need to check daily time limits
        if (focusDecision is AppLaunchDecision.Block) return focusDecision

        // Step 4: Daily time limit check (applies even in LIGHT focus)
        return evaluateTimeLimit(app) ?: focusDecision
    }

    /**
     * Returns a Block decision if today's daily time limit is exhausted, or null if no limit applies.
     */
    private suspend fun evaluateTimeLimit(app: InstalledApp): AppLaunchDecision.Block? {
        val limit = appTimeLimitRepository.getLimit(app.packageName) ?: return null
        if (!limit.isEnabled || limit.effectiveDailyLimitMinutes <= 0) return null

        val usedMinutes = usageStatsRepository.getTodayUsageMinutes(app.packageName)
        return if (usedMinutes >= limit.effectiveDailyLimitMinutes) {
            AppLaunchDecision.Block(
                packageName = app.packageName,
                reason = "Daily limit of ${limit.formattedEffectiveLimit} reached for today (${usedMinutes}m used).",
                activeSession = null
            )
        } else null
    }
}
