package com.minimalphone.core.model

enum class AppCategory {
    ESSENTIAL,
    MANAGED,
    SYSTEM_INTERNAL,
    HIDDEN
}

enum class ClassificationSource {
    AUTOMATIC_DEFAULT,
    USER_OVERRIDE,
    SYSTEM_PRESET,
    WEB_APP
}

data class InstalledApp(
    val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory = AppCategory.MANAGED,
    val isSystemApp: Boolean = false,
    val isEssential: Boolean = false,
    val isBlockedInFocus: Boolean = true,
    val isFavoriteOnHome: Boolean = false,
    val iconKey: String? = null,
    val lastUsedTimestamp: Long = 0L,
    val installedTimestamp: Long = 0L,
    val familyId: String? = null,
    val quarantineUntil: Long? = null,
    val isClone: Boolean = false
)

/**
 * Domain model for an app that has been explicitly granted emergency/essential access.
 * These apps bypass focus restrictions and budget rules unconditionally.
 */
data class EssentialApp(
    val packageName: String,
    val label: String,
    /** True if this was set by the system bootstrapper; false if user-added. */
    val isSystemDefault: Boolean = false,
    val addedTimestamp: Long = 0L
)

enum class SettingsGuardMode {
    OFF,
    DEEP,
    DEEP_AND_STRICT
}

enum class QuarantineHours(val hours: Int?) {
    OFF(null),
    ONE(1),
    TWENTY_FOUR(24),
    SEVENTY_TWO(72)
}

data class AppFamily(
    val familyId: String,
    val packages: List<String>,
    val sharedLimit: Boolean = true,
    val mirrorFocusBlock: Boolean = true
)
