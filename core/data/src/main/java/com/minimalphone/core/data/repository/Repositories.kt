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
    val isDumbModeEnabled: Flow<Boolean>
    val isAutoGrayscaleInFocusEnabled: Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setAdaptiveFrictionEnabled(enabled: Boolean)
    suspend fun setDefaultFocusDurationMinutes(minutes: Int)
    suspend fun setLocalAnalyticsEnabled(enabled: Boolean)
    suspend fun setAppTheme(theme: com.minimalphone.core.model.AppTheme)
    suspend fun setDumbModeEnabled(enabled: Boolean)
    suspend fun setAutoGrayscaleInFocusEnabled(enabled: Boolean)
}

/**
 * Manages the set of user-configured essential (always-available) packages.
 * System-level emergency packages are handled directly by EmergencyAccessManager.
 */
interface EssentialAppRepository {
    /** Observe the full list of essential apps for the settings UI. */
    fun observeEssentialApps(): Flow<List<com.minimalphone.core.model.EssentialApp>>
    /** Returns the current set of essential package names (snapshot). */
    suspend fun getEssentialPackages(): Set<String>
    /** Reactive version for streaming changes. */
    fun observeEssentialPackages(): Flow<Set<String>>
    /** Add or update an essential app entry. */
    suspend fun addEssentialApp(packageName: String, label: String, isSystemDefault: Boolean = false)
    /** Remove a user-added essential app (system defaults cannot be removed). */
    suspend fun removeEssentialApp(packageName: String)
    /** Seed the initial set of essential apps (called once by bootstrapper). */
    suspend fun seedDefaults(apps: List<Pair<String, String>>)
}

/**
 * Manages daily app usage budgets.
 * Budget checks are evaluated by the RuleEngine (M13) as step 4 in the priority chain.
 */
interface BudgetRepository {
    fun observeAllBudgets(): Flow<List<com.minimalphone.core.model.AppBudget>>
    suspend fun getBudget(packageName: String): com.minimalphone.core.model.AppBudget?
    suspend fun setBudget(packageName: String, appLabel: String, dailyLimitMinutes: Int)
    suspend fun removeBudget(packageName: String)
    suspend fun getTodayUsageMinutes(packageName: String): Int
    suspend fun recordUsage(packageName: String, date: String, foregroundMinutes: Int, launchCount: Int)
    suspend fun incrementBlockedAttempt(packageName: String, date: String)
    fun observeTodayUsage(date: String): Flow<List<com.minimalphone.core.model.DailyAppUsage>>
}

/**
 * Manages automated focus schedules and named presets (M15).
 */
interface ScheduleRepository {
    fun observePresets(): Flow<List<com.minimalphone.core.model.FocusPreset>>
    suspend fun getPreset(id: String): com.minimalphone.core.model.FocusPreset?
    suspend fun savePreset(preset: com.minimalphone.core.model.FocusPreset)
    suspend fun deletePreset(id: String)
    suspend fun seedDefaultPresets()

    fun observeSchedules(): Flow<List<com.minimalphone.core.model.FocusSchedule>>
    fun observeActiveSchedules(): Flow<List<com.minimalphone.core.model.FocusSchedule>>
    suspend fun getActiveSchedules(): List<com.minimalphone.core.model.FocusSchedule>
    suspend fun getSchedule(id: String): com.minimalphone.core.model.FocusSchedule?
    suspend fun saveSchedule(schedule: com.minimalphone.core.model.FocusSchedule)
    suspend fun setScheduleEnabled(id: String, isEnabled: Boolean)
    suspend fun deleteSchedule(id: String)
}

/**
 * Manages batched notifications in the distraction-free Notification Digest (M17).
 */
interface NotificationDigestRepository {
    fun observeUnreadNotifications(): Flow<List<com.minimalphone.core.model.DigestNotification>>
    fun observeAllNotifications(): Flow<List<com.minimalphone.core.model.DigestNotification>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun recordNotification(packageName: String, appLabel: String, title: String?, text: String?)
    suspend fun markAllAsRead()
    suspend fun clearOldNotifications()
}

/**
 * Manages pinned quick-dial contacts (M20).
 */
interface ContactRepository {
    fun observePinnedContacts(): Flow<List<com.minimalphone.core.model.QuickContact>>
    fun observeAllContacts(): Flow<List<com.minimalphone.core.model.QuickContact>>
    suspend fun saveContact(name: String, phoneNumber: String, isPinned: Boolean = true)
    suspend fun deleteContact(id: String)
}





