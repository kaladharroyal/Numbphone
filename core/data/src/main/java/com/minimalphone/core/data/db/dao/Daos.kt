package com.minimalphone.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.data.db.entity.BlockedAttemptEntity
import com.minimalphone.core.data.db.entity.EssentialAppEntity
import com.minimalphone.core.data.db.entity.ExitAttemptEntity
import com.minimalphone.core.data.db.entity.FocusGoalEntity
import com.minimalphone.core.data.db.entity.FocusSessionEntity
import com.minimalphone.core.data.db.entity.InstalledAppEntity
import com.minimalphone.core.model.AppCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAppDao {

    @Query("SELECT * FROM installed_apps ORDER BY label COLLATE NOCASE ASC")
    fun observeAllApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE category = :category ORDER BY label COLLATE NOCASE ASC")
    fun observeAppsByCategory(category: AppCategory): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE isFavoriteOnHome = 1 OR isEssential = 1 ORDER BY label COLLATE NOCASE ASC")
    fun observeHomeApps(): Flow<List<InstalledAppEntity>>

    @Query("SELECT * FROM installed_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): InstalledAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApps(apps: List<InstalledAppEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApp(app: InstalledAppEntity)

    @Query("DELETE FROM installed_apps WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)

    @Query("DELETE FROM installed_apps WHERE packageName NOT IN (:packages)")
    suspend fun deleteAppsNotIn(packages: List<String>)

    @Query("UPDATE installed_apps SET category = :category, isEssential = :isEssential, isBlockedInFocus = :isBlocked WHERE packageName = :packageName")
    suspend fun updateCategory(packageName: String, category: AppCategory, isEssential: Boolean, isBlocked: Boolean)

    @Query("UPDATE installed_apps SET isFavoriteOnHome = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavorite(packageName: String, isFavorite: Boolean)
}

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules")
    fun observeAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRule(packageName: String): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppRuleEntity)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteRule(packageName: String)
}

@Dao
interface FocusSessionDao {

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSessionSync(): FocusSessionEntity?

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("UPDATE focus_sessions SET status = :status, exitFrictionSecondsCompleted = :frictionSeconds WHERE id = :sessionId")
    suspend fun updateSessionStatus(sessionId: String, status: String, frictionSeconds: Int)

    @Query("UPDATE focus_sessions SET bypassAttemptsCount = bypassAttemptsCount + 1 WHERE id = :sessionId")
    suspend fun incrementBypassAttempt(sessionId: String)

    @Query("SELECT * FROM focus_goals ORDER BY lastUsedTimestamp DESC LIMIT 10")
    fun observeRecentGoals(): Flow<List<FocusGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: FocusGoalEntity)
}

@Dao
interface BlockedAttemptDao {

    @Query("SELECT * FROM blocked_attempts ORDER BY timestamp DESC")
    fun observeAllBlocked(): Flow<List<BlockedAttemptEntity>>

    @Query("SELECT COUNT(*) FROM blocked_attempts WHERE timestamp >= :sinceTimestamp")
    fun observeBlockedCountSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: BlockedAttemptEntity)
}

@Dao
interface ExitAttemptDao {

    @Query("SELECT * FROM exit_attempts WHERE focusSessionId = :sessionId ORDER BY timestamp ASC")
    fun observeExitAttemptsForSession(sessionId: String): Flow<List<ExitAttemptEntity>>

    @Query("SELECT COUNT(*) FROM exit_attempts WHERE focusSessionId = :sessionId")
    suspend fun getExitAttemptsCount(sessionId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: ExitAttemptEntity)
}

@Dao
interface EssentialAppDao {

    @Query("SELECT * FROM essential_apps ORDER BY label COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<EssentialAppEntity>>

    @Query("SELECT packageName FROM essential_apps")
    suspend fun getAllPackageNames(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: EssentialAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(apps: List<EssentialAppEntity>)

    @Query("DELETE FROM essential_apps WHERE packageName = :packageName AND isSystemDefault = 0")
    suspend fun deleteUserAdded(packageName: String)

    @Query("SELECT COUNT(*) FROM essential_apps WHERE packageName = :packageName")
    suspend fun exists(packageName: String): Int
}

// ─────────── M14: Daily App Budgets ───────────

@Dao
interface AppBudgetDao {

    @Query("SELECT * FROM app_budgets WHERE enabled = 1 ORDER BY packageName")
    fun observeActiveBudgets(): Flow<List<com.minimalphone.core.data.db.entity.AppBudgetEntity>>

    @Query("SELECT * FROM app_budgets ORDER BY packageName")
    fun observeAllBudgets(): Flow<List<com.minimalphone.core.data.db.entity.AppBudgetEntity>>

    @Query("SELECT * FROM app_budgets WHERE packageName = :packageName LIMIT 1")
    suspend fun getBudget(packageName: String): com.minimalphone.core.data.db.entity.AppBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: com.minimalphone.core.data.db.entity.AppBudgetEntity)

    @Query("DELETE FROM app_budgets WHERE packageName = :packageName")
    suspend fun delete(packageName: String)
}

@Dao
interface DailyAppUsageDao {

    @Query("SELECT * FROM daily_app_usage WHERE date = :date ORDER BY foregroundMinutes DESC")
    fun observeUsageForDate(date: String): Flow<List<com.minimalphone.core.data.db.entity.DailyAppUsageEntity>>

    @Query("SELECT * FROM daily_app_usage WHERE packageName = :packageName AND date = :date LIMIT 1")
    suspend fun getUsage(packageName: String, date: String): com.minimalphone.core.data.db.entity.DailyAppUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(usage: com.minimalphone.core.data.db.entity.DailyAppUsageEntity)

    @Query("UPDATE daily_app_usage SET blockedAttempts = blockedAttempts + 1 WHERE packageName = :packageName AND date = :date")
    suspend fun incrementBlockedAttempts(packageName: String, date: String)

    @Query("DELETE FROM daily_app_usage WHERE date < :beforeDate")
    suspend fun deleteOlderThan(beforeDate: String)
}

// ─────────── M15: Scheduled Focus + Presets ───────────

@Dao
interface FocusPresetDao {

    @Query("SELECT * FROM focus_presets ORDER BY isDefault DESC, title ASC")
    fun observeAll(): Flow<List<com.minimalphone.core.data.db.entity.FocusPresetEntity>>

    @Query("SELECT * FROM focus_presets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): com.minimalphone.core.data.db.entity.FocusPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: com.minimalphone.core.data.db.entity.FocusPresetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(presets: List<com.minimalphone.core.data.db.entity.FocusPresetEntity>)

    @Query("DELETE FROM focus_presets WHERE id = :id AND isDefault = 0")
    suspend fun delete(id: String)
}

@Dao
interface FocusScheduleDao {

    @Query("SELECT * FROM focus_schedules ORDER BY startTimeHour, startTimeMinute ASC")
    fun observeAll(): Flow<List<com.minimalphone.core.data.db.entity.FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1")
    fun observeActiveSchedules(): Flow<List<com.minimalphone.core.data.db.entity.FocusScheduleEntity>>

    @Query("SELECT * FROM focus_schedules WHERE isEnabled = 1")
    suspend fun getActiveSchedules(): List<com.minimalphone.core.data.db.entity.FocusScheduleEntity>

    @Query("SELECT * FROM focus_schedules WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): com.minimalphone.core.data.db.entity.FocusScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: com.minimalphone.core.data.db.entity.FocusScheduleEntity)

    @Query("UPDATE focus_schedules SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: String, isEnabled: Boolean)

    @Query("DELETE FROM focus_schedules WHERE id = :id")
    suspend fun delete(id: String)
}

// ─────────── M17: Notification Digest ───────────

@Dao
interface DigestNotificationDao {

    @Query("SELECT * FROM digest_notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun observeUnreadNotifications(): Flow<List<com.minimalphone.core.data.db.entity.DigestNotificationEntity>>

    @Query("SELECT * FROM digest_notifications ORDER BY timestamp DESC LIMIT 100")
    fun observeAllNotifications(): Flow<List<com.minimalphone.core.data.db.entity.DigestNotificationEntity>>

    @Query("SELECT COUNT(*) FROM digest_notifications WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: com.minimalphone.core.data.db.entity.DigestNotificationEntity)

    @Query("UPDATE digest_notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM digest_notifications WHERE timestamp < :beforeTimestamp")
    suspend fun clearOlderThan(beforeTimestamp: Long)
}

// ─────────── M20: Quick Contacts ───────────

@Dao
interface QuickContactDao {

    @Query("SELECT * FROM quick_contacts WHERE isPinned = 1 ORDER BY addedTimestamp ASC")
    fun observePinnedContacts(): Flow<List<com.minimalphone.core.data.db.entity.QuickContactEntity>>

    @Query("SELECT * FROM quick_contacts ORDER BY name COLLATE NOCASE ASC")
    fun observeAllContacts(): Flow<List<com.minimalphone.core.data.db.entity.QuickContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: com.minimalphone.core.data.db.entity.QuickContactEntity)

    @Query("DELETE FROM quick_contacts WHERE id = :id")
    suspend fun delete(id: String)
}





