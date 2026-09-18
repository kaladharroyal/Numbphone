package com.minimalphone.core.domain.launch

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
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

class FakeLaunchTimeLimitRepository : AppTimeLimitRepository {
    private val limits = mutableMapOf<String, AppTimeLimit>()
    private val flow = MutableStateFlow<List<AppTimeLimit>>(emptyList())

    fun setLimitSync(limit: AppTimeLimit) {
        limits[limit.packageName] = limit
        flow.value = limits.values.toList()
    }

    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = flow.asStateFlow()
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits[packageName]
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(limits[packageName])
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        limits[packageName] = AppTimeLimit(packageName, limitMinutes, isEnabled)
        flow.value = limits.values.toList()
    }
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {
        val cur = limits[packageName] ?: AppTimeLimit(packageName, 0)
        limits[packageName] = cur.copy(emergencyExtensionMinutes = cur.emergencyExtensionMinutes + additionalMinutes)
        flow.value = limits.values.toList()
    }
    override suspend fun removeLimit(packageName: String) {
        limits.remove(packageName)
        flow.value = limits.values.toList()
    }
}

class FakeLaunchUsageStatsRepository : UsageStatsRepository {
    val usageMap = mutableMapOf<String, Long>()

    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = MutableStateFlow(DailyUsageSummary())
    override suspend fun hasUsagePermission(): Boolean = true
    override suspend fun getTodayUsageMinutes(packageName: String): Long = usageMap[packageName] ?: 0L
}

class LaunchEngineTest {

    private lateinit var appRepository: FakeLaunchAppRepository
    private lateinit var focusSessionRepository: FakeLaunchSessionRepository
    private lateinit var blockedAttemptRepository: FakeBlockedAttemptRepository
    private lateinit var appTimeLimitRepository: FakeLaunchTimeLimitRepository
    private lateinit var usageStatsRepository: FakeLaunchUsageStatsRepository
    private lateinit var evaluateAppLaunchUseCase: EvaluateAppLaunchUseCase
    private lateinit var launchAppUseCase: LaunchAppUseCase

    @Before
    fun setup() {
        appRepository = FakeLaunchAppRepository()
        focusSessionRepository = FakeLaunchSessionRepository()
        blockedAttemptRepository = FakeBlockedAttemptRepository()
        appTimeLimitRepository = FakeLaunchTimeLimitRepository()
        usageStatsRepository = FakeLaunchUsageStatsRepository()
        evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(
            appRepository,
            focusSessionRepository,
            appTimeLimitRepository,
            usageStatsRepository
        )
        launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, appRepository, blockedAttemptRepository)
    }

    @Test
    fun `essential app is allowed when focus is inactive`() = runBlocking {
        val decision = launchAppUseCase("com.google.android.dialer")
        assertTrue(decision is AppLaunchDecision.Allow)
        assertEquals("com.google.android.dialer", (decision as AppLaunchDecision.Allow).packageName)
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
        assertTrue(decision is AppLaunchDecision.Allow)
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

    @Test
    fun `managed app is allowed when daily usage is under time limit`() = runBlocking {
        appTimeLimitRepository.setLimitSync(AppTimeLimit("com.google.android.youtube", dailyLimitMinutes = 60, isEnabled = true))
        usageStatsRepository.usageMap["com.google.android.youtube"] = 45L

        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `managed app is blocked when daily usage reaches or exceeds time limit`() = runBlocking {
        appTimeLimitRepository.setLimitSync(AppTimeLimit("com.google.android.youtube", dailyLimitMinutes = 60, isEnabled = true))
        usageStatsRepository.usageMap["com.google.android.youtube"] = 65L

        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.Block)
        val block = decision as AppLaunchDecision.Block
        assertTrue(block.reason.contains("Daily limit"))
        assertEquals(1, blockedAttemptRepository.recordedAttempts.size)
    }

    @Test
    fun `essential app with explicit time limit is blocked when limit exceeded`() = runBlocking {
        appTimeLimitRepository.setLimitSync(AppTimeLimit("com.google.android.dialer", dailyLimitMinutes = 30, isEnabled = true))
        usageStatsRepository.usageMap["com.google.android.dialer"] = 50L

        val decision = launchAppUseCase("com.google.android.dialer")
        assertTrue(decision is AppLaunchDecision.Block)
        val block = decision as AppLaunchDecision.Block
        assertTrue(block.reason.contains("Daily limit"))
    }

    @Test
    fun `managed app is allowed after adding emergency extension when over initial daily limit`() = runBlocking {
        appTimeLimitRepository.setLimitSync(
            AppTimeLimit(
                packageName = "com.google.android.youtube",
                dailyLimitMinutes = 60,
                emergencyExtensionMinutes = 15,
                isEnabled = true
            )
        )
        // 65 min used is > 60 min, but <= 75 min effective limit
        usageStatsRepository.usageMap["com.google.android.youtube"] = 65L

        val decision = launchAppUseCase("com.google.android.youtube")
        assertTrue(decision is AppLaunchDecision.Allow)
    }
}
