package com.minimalphone.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.ClassificationSource
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.SessionStatus

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val activityName: String,
    val label: String,
    val category: AppCategory,
    val isSystemApp: Boolean,
    val isEssential: Boolean,
    val isBlockedInFocus: Boolean,
    val isFavoriteOnHome: Boolean,
    val iconKey: String? = null,
    val lastUsedTimestamp: Long = 0L,
    val installedTimestamp: Long = System.currentTimeMillis(),
    val classificationSource: ClassificationSource = ClassificationSource.AUTOMATIC_DEFAULT
)

@Entity(tableName = "app_rules")
data class AppRuleEntity(
    @PrimaryKey val packageName: String,
    val category: AppCategory,
    val isAlwaysAvailable: Boolean,
    val isBlockedInFocus: Boolean,
    val isFavoriteOnHome: Boolean,
    val customLabel: String? = null,
    val classificationSource: ClassificationSource = ClassificationSource.USER_OVERRIDE,
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val startTime: Long,
    val endTime: Long,
    val durationMinutes: Int,
    val goalId: String,
    val goalTitle: String,
    val goalCategory: String,
    val mode: FocusMode,
    val status: SessionStatus,
    val bypassAttemptsCount: Int = 0,
    val exitFrictionSecondsCompleted: Int = 0
)

@Entity(tableName = "blocked_attempts")
data class BlockedAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val timestamp: Long = System.currentTimeMillis(),
    val focusSessionId: String?,
    val route: String,
    val userIntentReason: String? = null
)

@Entity(tableName = "exit_attempts")
data class ExitAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val focusSessionId: String,
    val attemptNumber: Int,
    val requiredFrictionSeconds: Int,
    val completedFrictionSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccessfulExit: Boolean = false,
    val exitReason: String? = null
)

@Entity(tableName = "focus_goals")
data class FocusGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val isCustom: Boolean,
    val usageCount: Int = 1,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Stores user-configured essential packages.
 * These bypass all focus restrictions and budget rules.
 * System-level emergency packages are hardcoded in EmergencyAccessManager
 * and never stored in the database.
 */
@Entity(tableName = "essential_apps")
data class EssentialAppEntity(
    @PrimaryKey val packageName: String,
    /** Human-readable label for display in the Essential Access settings UI. */
    val label: String,
    /** Whether this entry was added by the system bootstrapper (true) or the user (false). */
    val isSystemDefault: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis()
)

// ─────────── M14: Daily App Budgets ───────────

/**
 * User-defined daily usage limit per app.
 * When the app's foreground time today exceeds [dailyLimitMinutes], the RuleEngine blocks it.
 */
@Entity(tableName = "app_budgets")
data class AppBudgetEntity(
    @PrimaryKey val packageName: String,
    /** 0 = disabled. Positive value = daily limit in minutes. */
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true,
    val createdTimestamp: Long = System.currentTimeMillis()
)

/**
 * Tracks how long each app was used on a given calendar day.
 * Populated by a background sync from UsageStatsManager each time the RuleEngine evaluates.
 */
@Entity(tableName = "daily_app_usage", primaryKeys = ["packageName", "date"])
data class DailyAppUsageEntity(
    val packageName: String,
    /** ISO-8601 calendar date, e.g. "2026-09-18". */
    val date: String,
    val foregroundMinutes: Int = 0,
    val launchCount: Int = 0,
    val blockedAttempts: Int = 0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)

// ─────────── M15: Scheduled Focus + Presets ───────────

@Entity(tableName = "focus_presets")
data class FocusPresetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val durationMinutes: Int,
    val mode: FocusMode,
    val category: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "focus_schedules")
data class FocusScheduleEntity(
    @PrimaryKey val id: String,
    val presetId: String? = null,
    val title: String,
    /** Comma-separated DayOfWeek names, e.g. "MONDAY,TUESDAY,WEDNESDAY" */
    val daysOfWeek: String,
    val startTimeHour: Int,
    val startTimeMinute: Int,
    val durationMinutes: Int,
    val mode: FocusMode,
    val isEnabled: Boolean = true
)

// ─────────── M17: Notification Digest ───────────

@Entity(tableName = "digest_notifications")
data class DigestNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val packageName: String,
    val appLabel: String,
    val title: String?,
    val text: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ─────────── M20: Quick Contacts ───────────

@Entity(tableName = "quick_contacts")
data class QuickContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumber: String,
    val isPinned: Boolean = true,
    val addedTimestamp: Long = System.currentTimeMillis()
)





