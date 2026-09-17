package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAppRepositoryForBackup : AppRepository {
    val apps = mutableListOf(
        InstalledApp(packageName = "com.phone.essential", activityName = "MainActivity", label = "Phone", category = AppCategory.ESSENTIAL, isEssential = true),
        InstalledApp(packageName = "com.social.distraction", activityName = "MainActivity", label = "Social", category = AppCategory.MANAGED, isEssential = false)
    )
    private val appsFlow = MutableStateFlow<List<InstalledApp>>(apps)

    override fun getAllApps(): Flow<List<InstalledApp>> = appsFlow
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override fun getManagedApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override fun getHomeApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override suspend fun getApp(packageName: String): InstalledApp? = apps.find { it.packageName == packageName }
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            apps[index] = apps[index].copy(category = category)
            appsFlow.value = apps.toList()
        }
    }
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index >= 0) {
            apps[index] = apps[index].copy(isFavoriteOnHome = isFavorite)
            appsFlow.value = apps.toList()
        }
    }
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeFocusSessionRepositoryForBackup : FocusSessionRepository {
    val sessions = mutableListOf<FocusSession>()
    private val sessionsFlow = MutableStateFlow<List<FocusSession>>(sessions)

    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(null)
    override suspend fun getActiveSessionSync(): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = sessionsFlow
    override suspend fun startSession(session: FocusSession) {
        sessions.add(session)
        sessionsFlow.value = sessions.toList()
    }
    override suspend fun completeSession(sessionId: String) {
        val index = sessions.indexOfFirst { it.id == sessionId }
        if (index >= 0) {
            sessions[index] = sessions[index].copy(status = com.minimalphone.core.model.SessionStatus.COMPLETED)
            sessionsFlow.value = sessions.toList()
        }
    }
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {}
    override suspend fun incrementBypassAttempt(sessionId: String) {}
    override fun getRecentGoals() = MutableStateFlow(emptyList<com.minimalphone.core.model.FocusGoal>())
}

class BackupUseCasesTest {

    private lateinit var appRepository: FakeAppRepositoryForBackup
    private lateinit var focusSessionRepository: FakeFocusSessionRepositoryForBackup
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var exportBackupJsonUseCase: ExportBackupJsonUseCase
    private lateinit var importBackupJsonUseCase: ImportBackupJsonUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepositoryForBackup()
        focusSessionRepository = FakeFocusSessionRepositoryForBackup()
        settingsRepository = FakeSettingsRepository()
        exportBackupJsonUseCase = ExportBackupJsonUseCase(appRepository, focusSessionRepository, settingsRepository)
        importBackupJsonUseCase = ImportBackupJsonUseCase(appRepository, focusSessionRepository, settingsRepository)
    }

    @Test
    fun exportBackup_producesValidJsonWithAppsAndSettings() = runTest {
        settingsRepository.setAdaptiveFrictionEnabled(true)
        settingsRepository.setDefaultFocusDurationMinutes(45)
        settingsRepository.setAppTheme(AppTheme.WARM_AMBER)

        val result = exportBackupJsonUseCase()
        assertTrue(result.jsonString.contains("com.phone.essential"))
        assertTrue(result.jsonString.contains("WARM_AMBER"))
        assertTrue(result.jsonString.contains("\"defaultFocusDurationMinutes\": 45"))
        assertEquals(2, result.totalApps)
    }

    @Test
    fun importBackup_restoresSettingsAndAppRules() = runTest {
        val backupJson = """
            {
              "version": 1,
              "timestamp": 1726574400000,
              "settings": {
                "isOnboardingCompleted": true,
                "isAdaptiveFrictionEnabled": false,
                "defaultFocusDurationMinutes": 60,
                "isLocalAnalyticsEnabled": false,
                "appTheme": "E_INK_PAPER"
              },
              "apps": [
                {
                  "packageName": "com.social.distraction",
                  "category": "ESSENTIAL",
                  "isFavoriteOnHome": true
                }
              ]
            }
        """.trimIndent()

        val importResult = importBackupJsonUseCase(backupJson)
        assertTrue(importResult.isSuccess)
        assertEquals(1, importResult.restoredAppsCount)

        val restoredApp = appRepository.getApp("com.social.distraction")
        assertEquals(AppCategory.ESSENTIAL, restoredApp?.category)
        assertEquals(true, restoredApp?.isFavoriteOnHome)
        assertEquals(AppTheme.E_INK_PAPER, settingsRepository.appThemeFlow.value)
        assertEquals(60, settingsRepository.defaultDurationFlow.value)
    }
}
