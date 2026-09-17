package com.minimalphone.core.data.repository

import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppRule
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.ExitAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getAllApps(): Flow<List<InstalledApp>>
    fun getAlwaysAvailableApps(): Flow<List<InstalledApp>>
    fun getManagedApps(): Flow<List<InstalledApp>>
    fun getHomeApps(): Flow<List<InstalledApp>>
    suspend fun getApp(packageName: String): InstalledApp?
    suspend fun syncInstalledApps()
    suspend fun updateAppCategory(packageName: String, category: AppCategory)
    suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean)
    suspend fun onPackageAdded(packageName: String)
    suspend fun onPackageRemoved(packageName: String)
    suspend fun onPackageChanged(packageName: String)
}

interface FocusSessionRepository {
    fun getActiveSession(): Flow<FocusSession?>
    suspend fun getActiveSessionSync(): FocusSession?
    fun getAllSessions(): Flow<List<FocusSession>>
    suspend fun startSession(session: FocusSession)
    suspend fun completeSession(sessionId: String)
    suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?)
    suspend fun incrementBypassAttempt(sessionId: String)
    fun getRecentGoals(): Flow<List<FocusGoal>>
}

interface BlockedAttemptRepository {
    fun getBlockedAttempts(): Flow<List<BlockedAttempt>>
    fun getTodayBlockedCount(): Flow<Int>
    suspend fun recordBlockedAttempt(attempt: BlockedAttempt)
}

interface ExitAttemptRepository {
    fun getExitAttemptsForSession(sessionId: String): Flow<List<ExitAttempt>>
    suspend fun getExitAttemptsCount(sessionId: String): Int
    suspend fun recordExitAttempt(attempt: ExitAttempt)
}

interface SettingsRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val isAdaptiveFrictionEnabled: Flow<Boolean>
    val defaultFocusDurationMinutes: Flow<Int>
    val isLocalAnalyticsEnabled: Flow<Boolean>
    val appTheme: Flow<com.minimalphone.core.model.AppTheme>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setAdaptiveFrictionEnabled(enabled: Boolean)
    suspend fun setDefaultFocusDurationMinutes(minutes: Int)
    suspend fun setLocalAnalyticsEnabled(enabled: Boolean)
    suspend fun setAppTheme(theme: com.minimalphone.core.model.AppTheme)
}
