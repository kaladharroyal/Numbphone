package com.minimalphone.core.model

enum class FocusMode {
    LIGHT,
    STRICT,
    DEEP_FOCUS
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED_WITH_FRICTION
}

data class FocusGoal(
    val id: String,
    val title: String,
    val category: String = "Study",
    val isCustom: Boolean = false
)

data class FocusSession(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val goal: FocusGoal,
    val mode: FocusMode = FocusMode.STRICT,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val bypassAttemptsCount: Int = 0,
    val exitFrictionSecondsCompleted: Int = 0
) {
    val isCurrentlyActive: Boolean
        get() = status == SessionStatus.ACTIVE && System.currentTimeMillis() < endTime

    val remainingMillis: Long
        get() = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
}
