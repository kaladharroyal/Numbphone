package com.minimalphone.core.model

enum class PauseStyle {
    BREATHE,
    INTENTION,
    DELAY_ONLY;

    companion object {
        fun fromName(name: String?): PauseStyle = when (name?.uppercase()) {
            "INTENTION" -> INTENTION
            "DELAY_ONLY" -> DELAY_ONLY
            else -> BREATHE
        }
    }
}

data class PauseConfig(
    val packageName: String,
    val enabled: Boolean = true,
    val pauseSeconds: Int = 5,
    val style: PauseStyle = PauseStyle.BREATHE,
    val sessionMinutes: Int = 10,
    val reinterventionEnabled: Boolean = true
)

enum class PauseEventKind {
    LAUNCH,
    REINTERVENTION;

    companion object {
        fun fromName(name: String?): PauseEventKind = when (name?.uppercase()) {
            "REINTERVENTION" -> REINTERVENTION
            else -> LAUNCH
        }
    }
}

enum class PauseOutcome {
    PROCEEDED,
    LEFT;

    companion object {
        fun fromName(name: String?): PauseOutcome = when (name?.uppercase()) {
            "LEFT" -> LEFT
            else -> PROCEEDED
        }
    }
}

data class PauseEvent(
    val id: Long = 0L,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val kind: PauseEventKind = PauseEventKind.LAUNCH,
    val outcome: PauseOutcome = PauseOutcome.PROCEEDED,
    val reflectionReason: String? = null,
    val secondsShown: Int = 0
)
