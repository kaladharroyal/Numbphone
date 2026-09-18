package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.ScheduleRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.EssentialApp
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusPreset
import com.minimalphone.core.model.FocusSchedule
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeAppRepositoryForBackup : AppRepository {
    private val apps = mutableMapOf(
        "com.phone.essential" to InstalledApp(
            packageName = "com.phone.essential",
            activityName = "PhoneActivity",
            label = "Phone",
            category = AppCategory.ESSENTIAL,
            isEssential = true,
            isFavoriteOnHome = true
        ),
        "com.social.distraction" to InstalledApp(
            packageName = "com.social.distraction",
            activityName = "SocialActivity",
            label = "Social",
            category = AppCategory.MANAGED,
            isEssential = false,
            isFavoriteOnHome = false
        )
    )
    private val flow = MutableStateFlow(apps.values.toList())

    override fun getAllApps(): Flow<List<InstalledApp>> = flow.asStateFlow()
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = flow.asStateFlow()
    override fun getManagedApps(): Flow<List<InstalledApp>> = flow.asStateFlow()
    override fun getHomeApps(): Flow<List<InstalledApp>> = flow.asStateFlow()
    override suspend fun getApp(packageName: String): InstalledApp? = apps[packageName]
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {
        apps[packageName]?.let { apps[packageName] = it.copy(category = category) }
        flow.value = apps.values.toList()
    }
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {
        apps[packageName]?.let { apps[packageName] = it.copy(isFavoriteOnHome = isFavorite) }
        flow.value = apps.values.toList()
    }
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeFocusSessionRepositoryForBackup : FocusSessionRepository {
    private val sessions = mutableListOf<FocusSession>()
    private val flow = MutableStateFlow<List<FocusSession>>(sessions)

    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(null)
    override suspend fun getActiveSessionSync(): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = flow
    override suspend fun startSession(session: FocusSession) {
        sessions.add(session)
        flow.value = sessions.toList()
    }
    override suspend fun completeSession(sessionId: String) {
        val idx = sessions.indexOfFirst { it.id == sessionId }
        if (idx != -1) {
            sessions[idx] = sessions[idx].copy(status = SessionStatus.COMPLETED)
            flow.value = sessions.toList()
        }
    }
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {}
    override suspend fun incrementBypassAttempt(sessionId: String) {}
    override fun getRecentGoals() = MutableStateFlow(emptyList<com.minimalphone.core.model.FocusGoal>())
}

class FakeTimeLimitRepositoryForBackup : AppTimeLimitRepository {
    private val limits = mutableListOf<AppTimeLimit>()
    private val flow = MutableStateFlow<List<AppTimeLimit>>(limits)

    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = flow
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits.find { it.packageName == packageName }
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(limits.find { it.packageName == packageName })
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        limits.removeAll { it.packageName == packageName }
        limits.add(AppTimeLimit(packageName, limitMinutes, isEnabled))
        flow.value = limits.toList()
    }
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {}
    override suspend fun removeLimit(packageName: String) {
        limits.removeAll { it.packageName == packageName }
        flow.value = limits.toList()
    }
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
    override suspend fun getActiveSchedules(): List<FocusSchedule> = schedules
    override suspend fun getSchedule(id: String): FocusSchedule? = schedules.find { it.id == id }
    override suspend fun saveSchedule(schedule: FocusSchedule) {
        schedules.removeAll { it.id == schedule.id }
        schedules.add(schedule)
        schedulesFlow.value = schedules.toList()
    }
    override suspend fun setScheduleEnabled(id: String, isEnabled: Boolean) {}
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
        essentials.removeAll { it.packageName == packageName }
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
        contacts.add(QuickContact(java.util.UUID.randomUUID().toString(), name, phoneNumber, isPinned))
        flow.value = contacts.toList()
    }
    override suspend fun deleteContact(id: String) {
        contacts.removeAll { it.id == id }
        flow.value = contacts.toList()
    }
}

class FakeSettingsRepositoryForBackup : SettingsRepository {
    val themeFlow = MutableStateFlow(AppTheme.PURE_BLACK)
    val durationFlow = MutableStateFlow(25)
    val frictionFlow = MutableStateFlow(true)
    val onboardingFlow = MutableStateFlow(false)
    val analyticsFlow = MutableStateFlow(true)
    val dumbModeFlow = MutableStateFlow(false)

    override val isOnboardingCompleted: Flow<Boolean> = onboardingFlow
    override val isAdaptiveFrictionEnabled: Flow<Boolean> = frictionFlow
    override val defaultFocusDurationMinutes: Flow<Int> = durationFlow
    override val isLocalAnalyticsEnabled: Flow<Boolean> = analyticsFlow
    override val appTheme: Flow<AppTheme> = themeFlow
    override val isDumbModeEnabled: Flow<Boolean> = dumbModeFlow
    override val isAutoGrayscaleInFocusEnabled: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) { onboardingFlow.value = completed }
    override suspend fun setAdaptiveFrictionEnabled(enabled: Boolean) { frictionFlow.value = enabled }
    override suspend fun setDefaultFocusDurationMinutes(minutes: Int) { durationFlow.value = minutes }
    override suspend fun setLocalAnalyticsEnabled(enabled: Boolean) { analyticsFlow.value = enabled }
    override suspend fun setAppTheme(theme: AppTheme) { themeFlow.value = theme }
    override suspend fun setDumbModeEnabled(enabled: Boolean) { dumbModeFlow.value = enabled }
    override suspend fun setAutoGrayscaleInFocusEnabled(enabled: Boolean) {}
}

class BackupUseCasesTest {

    private lateinit var appRepository: FakeAppRepositoryForBackup
    private lateinit var focusSessionRepository: FakeFocusSessionRepositoryForBackup
    private lateinit var settingsRepository: FakeSettingsRepositoryForBackup
    private lateinit var timeLimitRepository: FakeTimeLimitRepositoryForBackup
    private lateinit var scheduleRepository: FakeScheduleRepositoryForBackup
    private lateinit var essentialAppRepository: FakeEssentialAppRepositoryForBackup
    private lateinit var contactRepository: FakeContactRepositoryForBackup

    private lateinit var exportBackupJsonUseCase: ExportBackupJsonUseCase
    private lateinit var importBackupJsonUseCase: ImportBackupJsonUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepositoryForBackup()
        focusSessionRepository = FakeFocusSessionRepositoryForBackup()
        settingsRepository = FakeSettingsRepositoryForBackup()
        timeLimitRepository = FakeTimeLimitRepositoryForBackup()
        scheduleRepository = FakeScheduleRepositoryForBackup()
        essentialAppRepository = FakeEssentialAppRepositoryForBackup()
        contactRepository = FakeContactRepositoryForBackup()

        exportBackupJsonUseCase = ExportBackupJsonUseCase(
            appRepository,
            focusSessionRepository,
            settingsRepository,
            timeLimitRepository,
            scheduleRepository,
            essentialAppRepository,
            contactRepository
        )
        importBackupJsonUseCase = ImportBackupJsonUseCase(
            appRepository,
            focusSessionRepository,
            settingsRepository,
            timeLimitRepository,
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
        assertEquals(AppTheme.E_INK_PAPER, settingsRepository.themeFlow.value)
        assertEquals(60, settingsRepository.durationFlow.value)
    }
}
