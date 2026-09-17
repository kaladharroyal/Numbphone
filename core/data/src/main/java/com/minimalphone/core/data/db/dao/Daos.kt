package com.minimalphone.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.data.db.entity.BlockedAttemptEntity
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
