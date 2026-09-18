package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BudgetRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.ScheduleRepository
import com.minimalphone.core.model.AppBudget
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.DailyAppUsage
import com.minimalphone.core.model.EssentialApp
import com.minimalphone.core.model.FocusPreset
import com.minimalphone.core.model.FocusSchedule
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
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

class FakeBudgetRepositoryForBackup : BudgetRepository {
    private val budgets = mutableListOf<AppBudget>()
    private val flow = MutableStateFlow<List<AppBudget>>(budgets)

    override fun observeAllBudgets(): Flow<List<AppBudget>> = flow
    override suspend fun getBudget(packageName: String): AppBudget? = budgets.find { it.packageName == packageName }
    override suspend fun setBudget(packageName: String, appLabel: String, dailyLimitMinutes: Int) {
        budgets.removeAll { it.packageName == packageName }
        budgets.add(AppBudget(packageName, appLabel, dailyLimitMinutes))
        flow.value = budgets.toList()
    }
    override suspend fun removeBudget(packageName: String) {
        budgets.removeAll { it.packageName == packageName }
        flow.value = budgets.toList()
    }
    override suspend fun getTodayUsageMinutes(packageName: String): Int = 0
    override suspend fun recordUsage(packageName: String, date: String, foregroundMinutes: Int, launchCount: Int) {}
    override suspend fun incrementBlockedAttempt(packageName: String, date: String) {}
    override fun observeTodayUsage(date: String): Flow<List<DailyAppUsage>> = MutableStateFlow(emptyList())
}

class FakeScheduleRepositoryForBackup : ScheduleRepository {
    private val presets = mutableListOf<FocusPreset>()
    private val schedules = mutableListOf<FocusSchedule>()
    private val presetsFlow = MutableStateFlow<List<FocusPreset>>(presets)
    private val schedulesFlow = MutableStateFlow<List<FocusSchedule>>(schedules)

    override fun observePresets(): Flow<List<FocusPreset>> = presetsFlow
    override suspend fun getPreset(id: String): FocusPreset? = presets.find { it.id == id }
    override suspend fun savePreset(preset: FocusPreset) {
        presets.removeAll { it.id == preset.id }
        presets.add(preset)
        presetsFlow.value = presets.toList()
    }
    override suspend fun deletePreset(id: String) {
        presets.removeAll { it.id == id }
        presetsFlow.value = presets.toList()
    }
    override suspend fun seedDefaultPresets() {}

    override fun observeSchedules(): Flow<List<FocusSchedule>> = schedulesFlow
    override fun observeActiveSchedules(): Flow<List<FocusSchedule>> = schedulesFlow
    override suspend fun getActiveSchedules(): List<FocusSchedule> = schedules.filter { it.isEnabled }
    override suspend fun getSchedule(id: String): FocusSchedule? = schedules.find { it.id == id }
    override suspend fun saveSchedule(schedule: FocusSchedule) {
        schedules.removeAll { it.id == schedule.id }
        schedules.add(schedule)
        schedulesFlow.value = schedules.toList()
    }
    override suspend fun setScheduleEnabled(id: String, isEnabled: Boolean) {
        val index = schedules.indexOfFirst { it.id == id }
        if (index >= 0) {
            schedules[index] = schedules[index].copy(isEnabled = isEnabled)
            schedulesFlow.value = schedules.toList()
        }
    }
    override suspend fun deleteSchedule(id: String) {
        schedules.removeAll { it.id == id }
        schedulesFlow.value = schedules.toList()
    }
}

class FakeEssentialAppRepositoryForBackup : EssentialAppRepository {
    private val essentials = mutableListOf<EssentialApp>()
    private val flow = MutableStateFlow<List<EssentialApp>>(essentials)

    override fun observeEssentialApps(): Flow<List<EssentialApp>> = flow
    override suspend fun getEssentialPackages(): Set<String> = essentials.map { it.packageName }.toSet()
    override fun observeEssentialPackages(): Flow<Set<String>> = MutableStateFlow(essentials.map { it.packageName }.toSet())
    override suspend fun addEssentialApp(packageName: String, label: String, isSystemDefault: Boolean) {
        essentials.removeAll { it.packageName == packageName }
        essentials.add(EssentialApp(packageName, label, isSystemDefault))
        flow.value = essentials.toList()
    }
    override suspend fun removeEssentialApp(packageName: String) {
        essentials.removeAll { it.packageName == packageName && !it.isSystemDefault }
        flow.value = essentials.toList()
    }
    override suspend fun seedDefaults(apps: List<Pair<String, String>>) {}
}

class FakeContactRepositoryForBackup : ContactRepository {
    private val contacts = mutableListOf<QuickContact>()
    private val flow = MutableStateFlow<List<QuickContact>>(contacts)

    override fun observePinnedContacts(): Flow<List<QuickContact>> = flow
    override fun observeAllContacts(): Flow<List<QuickContact>> = flow
    override suspend fun saveContact(name: String, phoneNumber: String, isPinned: Boolean) {
        contacts.add(QuickContact(id = java.util.UUID.randomUUID().toString(), name = name, phoneNumber = phoneNumber, isPinned = isPinned))
        flow.value = contacts.toList()
    }
    override suspend fun deleteContact(id: String) {
        contacts.removeAll { it.id == id }
        flow.value = contacts.toList()
    }
}

class BackupUseCasesTest {

    private lateinit var appRepository: FakeAppRepositoryForBackup
    private lateinit var focusSessionRepository: FakeFocusSessionRepositoryForBackup
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var budgetRepository: FakeBudgetRepositoryForBackup
    private lateinit var scheduleRepository: FakeScheduleRepositoryForBackup
    private lateinit var essentialAppRepository: FakeEssentialAppRepositoryForBackup
    private lateinit var contactRepository: FakeContactRepositoryForBackup

    private lateinit var exportBackupJsonUseCase: ExportBackupJsonUseCase
    private lateinit var importBackupJsonUseCase: ImportBackupJsonUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepositoryForBackup()
        focusSessionRepository = FakeFocusSessionRepositoryForBackup()
        settingsRepository = FakeSettingsRepository()
        budgetRepository = FakeBudgetRepositoryForBackup()
        scheduleRepository = FakeScheduleRepositoryForBackup()
        essentialAppRepository = FakeEssentialAppRepositoryForBackup()
        contactRepository = FakeContactRepositoryForBackup()

        exportBackupJsonUseCase = ExportBackupJsonUseCase(
            appRepository,
            focusSessionRepository,
            settingsRepository,
            budgetRepository,
            scheduleRepository,
            essentialAppRepository,
            contactRepository
        )
        importBackupJsonUseCase = ImportBackupJsonUseCase(
            appRepository,
            focusSessionRepository,
            settingsRepository,
            budgetRepository,
            scheduleRepository,
            essentialAppRepository,
            contactRepository
        )
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
              "version": 2,
              "timestamp": 1726574400000,
              "settings": {
                "isOnboardingCompleted": true,
                "isAdaptiveFrictionEnabled": false,
                "defaultFocusDurationMinutes": 60,
                "isLocalAnalyticsEnabled": false,
                "appTheme": "E_INK_PAPER",
                "isDumbModeEnabled": false
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
