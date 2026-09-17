package com.minimalphone.core.model

data class AppRule(
    val packageName: String,
    val category: AppCategory,
    val isAlwaysAvailable: Boolean,
    val isBlockedInFocus: Boolean,
    val customLabel: String? = null,
    val classificationSource: ClassificationSource = ClassificationSource.AUTOMATIC_DEFAULT,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

data class BlockedAttempt(
    val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val focusSessionId: String?,
    val route: String = "LAUNCHER",
    val userIntentReason: String? = null
)

data class ExitAttempt(
    val id: Long = 0L,
    val focusSessionId: String,
    val attemptNumber: Int,
    val requiredFrictionSeconds: Int,
    val completedFrictionSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccessfulExit: Boolean = false,
    val exitReason: String? = null
)

sealed interface AppLaunchDecision {
    data class Allow(val packageName: String, val activityName: String) : AppLaunchDecision
    data class Block(val packageName: String, val reason: String, val activeSession: FocusSession?) : AppLaunchDecision
    data class ShowFriction(val packageName: String, val activeSession: FocusSession, val frictionSeconds: Int) : AppLaunchDecision
}

enum class BypassRoute {
    LAUNCHER,
    NOTIFICATION,
    RECENTS,
    DEEP_LINK,
    EXTERNAL_INTENT
}

sealed interface BypassDecision {
    data object Allow : BypassDecision
    data class InterceptAndRedirect(val packageName: String, val session: FocusSession) : BypassDecision
}

data class PendingExitAttempt(
    val sessionId: String,
    val attemptNumber: Int,
    val requiredFrictionSeconds: Int,
    val goal: FocusGoal,
    val remainingMinutes: Int
)

object IntentReflectionDefaults {
    val OPTIONS = listOf(
        "Important / Urgent",
        "Just checking",
        "I'm bored",
        "Someone sent me something",
        "Other"
    )
}
