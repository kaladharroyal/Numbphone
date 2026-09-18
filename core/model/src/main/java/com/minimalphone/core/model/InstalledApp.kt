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
    SYSTEM_PRESET
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
    val installedTimestamp: Long = 0L
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

