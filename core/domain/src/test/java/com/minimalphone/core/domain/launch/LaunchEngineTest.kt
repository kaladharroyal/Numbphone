package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import com.minimalphone.core.data.repository.BudgetRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.rules.RuleEngine
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeLaunchAppRepository : AppRepository {
    private val apps = mutableMapOf(
        "com.google.android.dialer" to InstalledApp(
            packageName = "com.google.android.dialer",
            activityName = "DialerActivity",
            label = "Phone",
            category = AppCategory.ESSENTIAL,
            isEssential = true,
            isBlockedInFocus = false
        ),
        "com.google.android.youtube" to InstalledApp(
            packageName = "com.google.android.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            category = AppCategory.MANAGED,
            isEssential = false,
            isBlockedInFocus = true
        )
    )

    override fun getAllApps(): Flow<List<InstalledApp>> = MutableStateFlow(apps.values.toList())
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = MutableStateFlow(apps.values.filter { it.isEssential })
    override fun getManagedApps(): Flow<List<InstalledApp>> = MutableStateFlow(apps.values.filter { !it.isEssential })
    override fun getHomeApps(): Flow<List<InstalledApp>> = MutableStateFlow(apps.values.toList())
    override suspend fun getApp(packageName: String): InstalledApp? = apps[packageName]
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {}
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {}
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeLaunchSessionRepository : FocusSessionRepository {
    var activeSession: FocusSession? = null
    private val sessionFlow = MutableStateFlow<FocusSession?>(null)

    override fun getActiveSession(): Flow<FocusSession?> = sessionFlow.asStateFlow()
    override suspend fun getActiveSessionSync(): FocusSession? = activeSession
    override fun getAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun startSession(session: FocusSession) {
        activeSession = session
        sessionFlow.value = session
    }
    override suspend fun completeSession(sessionId: String) {
        activeSession = null
        sessionFlow.value = null
    }
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {
        activeSession = null
        sessionFlow.value = null
    }
    override suspend fun incrementBypassAttempt(sessionId: String) {}
    override fun getRecentGoals(): Flow<List<FocusGoal>> = MutableStateFlow(emptyList())
}

class FakeBlockedAttemptRepository : BlockedAttemptRepository {
    val recordedAttempts = mutableListOf<BlockedAttempt>()
    private val blockedFlow = MutableStateFlow<List<BlockedAttempt>>(emptyList())

    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = blockedFlow.asStateFlow()
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(recordedAttempts.size)
    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {
        recordedAttempts.add(attempt)
        blockedFlow.value = recordedAttempts.toList()
    }
}

class LaunchEngineTest {

    private lateinit var appRepository: FakeLaunchAppRepository
    private lateinit var focusSessionRepository: FakeLaunchSessionRepository
    private lateinit var blockedAttemptRepository: FakeBlockedAttemptRepository
    private lateinit var emergencyAccessManager: EmergencyAccessManager
    private lateinit var ruleEngine: RuleEngine
    private lateinit var evaluateAppLaunchUseCase: EvaluateAppLaunchUseCase
    private lateinit var launchAppUseCase: LaunchAppUseCase

    @Before
    fun setup() {
        appRepository = FakeLaunchAppRepository()
        focusSessionRepository = FakeLaunchSessionRepository()
        blockedAttemptRepository = FakeBlockedAttemptRepository()
        emergencyAccessManager = mockk(relaxed = true)
        coEvery { emergencyAccessManager.isEmergencyOrEssential("com.google.android.dialer") } returns true

        val budgetRepo = mockk<BudgetRepository>(relaxed = true)
        val settingsRepo = mockk<SettingsRepository>(relaxed = true)
        ruleEngine = RuleEngine(budgetRepo, settingsRepo)
        evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(appRepository, focusSessionRepository, emergencyAccessManager, ruleEngine)
        launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, appRepository, blockedAttemptRepository)
    }

    @Test
    fun `essential app is allowed when focus is inactive`() = runBlocking {
        val decision = launchAppUseCase("com.google.android.dialer")
        assertTrue(decision is AppLaunchDecision.EmergencyAllow || decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `essential app is allowed when deep focus is active`() = runBlocking {
        focusSessionRepository.activeSession = FocusSession(
            id = "sess-1",
            startTime = System.currentTimeMillis() - 10000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Exam Study"),
            mode = FocusMode.DEEP_FOCUS,
            status = SessionStatus.ACTIVE
        )

        val decision = launchAppUseCase("com.google.android.dialer")
        assertTrue(decision is AppLaunchDecision.EmergencyAllow || decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `managed app is allowed when focus is inactive`() = runBlocking {
        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `managed app is blocked when strict focus is active and attempt is recorded`() = runBlocking {
        focusSessionRepository.activeSession = FocusSession(
            id = "sess-1",
            startTime = System.currentTimeMillis() - 10000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Exam Study"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )

        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.Block)
        assertEquals(1, blockedAttemptRepository.recordedAttempts.size)
        assertEquals("com.google.android.youtube", blockedAttemptRepository.recordedAttempts.first().packageName)
        assertEquals("YouTube", blockedAttemptRepository.recordedAttempts.first().appLabel)
    }

    @Test
    fun `managed app shows friction when light focus is active`() = runBlocking {
        focusSessionRepository.activeSession = FocusSession(
            id = "sess-1",
            startTime = System.currentTimeMillis() - 10000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Light Study"),
            mode = FocusMode.LIGHT,
            status = SessionStatus.ACTIVE
        )

        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.ShowFriction)
    }
}
