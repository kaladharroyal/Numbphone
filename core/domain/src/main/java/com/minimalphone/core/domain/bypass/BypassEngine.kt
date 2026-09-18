package com.minimalphone.core.domain.bypass

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.BypassDecision
import com.minimalphone.core.model.BypassRoute
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import javax.inject.Inject

class EvaluateBypassRouteUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val emergencyAccessManager: EmergencyAccessManager,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(packageName: String): BypassDecision {
        // 1. Ignore Minimal Phone launcher itself
        if (packageName == "com.minimalphone" || packageName.startsWith("com.minimalphone.")) {
            return BypassDecision.Allow
        }

        // 2. Ignore Android System UI, System Framework, and Input Methods to prevent launcher deadlock
        if (isSystemPackageWhitelisted(packageName)) {
            return BypassDecision.Allow
        }

        // 3. Emergency/Essential apps always bypass — check EmergencyAccessManager first
        if (emergencyAccessManager.isEmergencyOrEssential(packageName)) {
            return BypassDecision.Allow
        }

        // 4. Check Per-App Daily Time Limits (Checked first before Focus session logic)
        val timeLimit = appTimeLimitRepository.getLimit(packageName)
        if (timeLimit != null && timeLimit.isEnabled && timeLimit.effectiveDailyLimitMinutes > 0) {
            val usedMinutes = usageStatsRepository.getTodayUsageMinutes(packageName)
            if (usedMinutes >= timeLimit.effectiveDailyLimitMinutes) {
                val activeSession = focusSessionRepository.getActiveSessionSync()
                val sessionToUse = activeSession ?: FocusSession(
                    id = "limit_$packageName",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 60000,
                    durationMinutes = timeLimit.effectiveDailyLimitMinutes,
                    goal = FocusGoal("limit", "Daily Limit Reached", "Budget"),
                    mode = FocusMode.STRICT,
                    status = SessionStatus.ACTIVE
                )
                return BypassDecision.InterceptAndRedirect(
                    packageName = packageName,
                    session = sessionToUse
                )
            }
        }

        // 5. Query app metadata: Essential apps bypass Focus Sessions
        val app = appRepository.getApp(packageName)
        if (app != null && (app.category == AppCategory.ESSENTIAL || app.isEssential)) {
            return BypassDecision.Allow
        }

        // 5. Check if a Focus Session is currently active
        val activeSession = focusSessionRepository.getActiveSessionSync()
        if (activeSession == null || !activeSession.isCurrentlyActive) {
            return BypassDecision.Allow
        }

        // 6. In STRICT and DEEP_FOCUS modes, intercept managed apps
        return when (activeSession.mode) {
            FocusMode.LIGHT -> BypassDecision.Allow
            FocusMode.STRICT,
            FocusMode.DEEP_FOCUS -> BypassDecision.InterceptAndRedirect(
                packageName = packageName,
                session = activeSession
            )
        }
    }

    private fun isSystemPackageWhitelisted(packageName: String): Boolean {
        return packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName == "com.android.settings" ||
                packageName.startsWith("com.google.android.inputmethod") ||
                packageName.startsWith("com.android.inputmethod")
    }
}


class RecordBypassAttemptUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val blockedAttemptRepository: BlockedAttemptRepository,
    private val focusSessionRepository: FocusSessionRepository
) {
    suspend operator fun invoke(
        packageName: String,
        route: BypassRoute = BypassRoute.RECENTS,
        userIntentReason: String? = null
    ) {
        val app = appRepository.getApp(packageName)
        val label = app?.label ?: packageName.substringAfterLast('.')
        val activeSession = focusSessionRepository.getActiveSessionSync()

        blockedAttemptRepository.recordBlockedAttempt(
            BlockedAttempt(
                packageName = packageName,
                appLabel = label,
                timestamp = System.currentTimeMillis(),
                focusSessionId = activeSession?.id,
                route = route.name,
                userIntentReason = userIntentReason
            )
        )

        if (activeSession != null) {
            focusSessionRepository.incrementBypassAttempt(activeSession.id)
        }
    }
}
