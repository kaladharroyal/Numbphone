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
