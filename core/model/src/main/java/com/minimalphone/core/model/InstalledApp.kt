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
