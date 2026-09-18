package com.minimalphone.feature.homescreen

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.ExtendAppTimeLimitUseCase
import com.minimalphone.core.domain.GetHomeAppsUseCase
import com.minimalphone.core.domain.SyncInstalledAppsUseCase
import com.minimalphone.core.domain.friction.RecordIntentReflectionUseCase
import com.minimalphone.core.domain.launch.EvaluateAppLaunchUseCase
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import com.minimalphone.core.domain.timelimit.TimeLimitExpiryNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeHomeAppRepository : AppRepository {
    private val appsFlow = MutableStateFlow<List<InstalledApp>>(
        listOf(
            InstalledApp(
                packageName = "com.google.android.dialer",
                activityName = "DialerActivity",
                label = "Phone",
                category = AppCategory.ESSENTIAL,
                isEssential = true,
                isBlockedInFocus = false,
                isFavoriteOnHome = true
            ),
            InstalledApp(
                packageName = "com.google.android.youtube",
                activityName = "YouTubeActivity",
                label = "YouTube",
                category = AppCategory.MANAGED,
                isEssential = false,
                isBlockedInFocus = true,
                isFavoriteOnHome = true
            )
        )
    )

    override fun getAllApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getManagedApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getHomeApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override suspend fun getApp(packageName: String): InstalledApp? = appsFlow.value.find { it.packageName == packageName }
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {}
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {}
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeHomeFocusSessionRepository : FocusSessionRepository {
    var activeSession: FocusSession? = null
    val sessionFlow = MutableStateFlow<FocusSession?>(null)

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

class FakeHomeBlockedAttemptRepository : BlockedAttemptRepository {
    val recorded = mutableListOf<BlockedAttempt>()

    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = MutableStateFlow(recorded)
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(recorded.size)
    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {
        recorded.add(attempt)
    }
}

class FakeHomeTimeLimitRepository : AppTimeLimitRepository {
    val limits = mutableMapOf<String, AppTimeLimit>()
    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = MutableStateFlow(limits.values.toList())
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits[packageName]
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(limits[packageName])
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        limits[packageName] = AppTimeLimit(packageName, limitMinutes, isEnabled)
    }
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {
        val cur = limits[packageName] ?: AppTimeLimit(packageName, 0)
        limits[packageName] = cur.copy(emergencyExtensionMinutes = cur.emergencyExtensionMinutes + additionalMinutes)
    }
    override suspend fun removeLimit(packageName: String) {
        limits.remove(packageName)
    }
}

class FakeHomeUsageStatsRepository : UsageStatsRepository {
    val usageMap = mutableMapOf<String, Long>()
    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = MutableStateFlow(DailyUsageSummary())
    override suspend fun hasUsagePermission(): Boolean = true
    override suspend fun getTodayUsageMinutes(packageName: String): Long = usageMap[packageName] ?: 0L
}

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial uiState observes home apps flow and focus session`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        val state = viewModel.uiState.value
        assertEquals(2, state.visibleApps.size)
        assertTrue(state.visibleApps.any { it.label == "Phone" })
        assertFalse(state.isFocusActive)
    }

    @Test
    fun `strict focus session filters out managed apps from visibleApps`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        val activeSession = FocusSession(
            id = "sess-strict",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Exam Prep"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        fakeSessionRepo.activeSession = activeSession
        fakeSessionRepo.sessionFlow.value = activeSession

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        val state = viewModel.uiState.value
        assertTrue(state.isFocusActive)
        assertEquals(1, state.visibleApps.size)
        assertEquals("Phone", state.visibleApps.first().label)
    }

    @Test
    fun `clicking managed app during focus triggers blocked dialog state`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        val activeSession = FocusSession(
            id = "sess-1",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Exam Prep"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        fakeSessionRepo.activeSession = activeSession
        fakeSessionRepo.sessionFlow.value = activeSession

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        var didAllowLaunch = false
        val youtube = InstalledApp(
            packageName = "com.google.android.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            category = AppCategory.MANAGED,
            isEssential = false,
            isBlockedInFocus = true
        )

        viewModel.onAppClicked(youtube) { _, _ ->
            didAllowLaunch = true
        }

        assertFalse(didAllowLaunch)
        assertTrue(viewModel.uiState.value.blockedDialog.isShowing)
        assertEquals("com.google.android.youtube", viewModel.uiState.value.blockedDialog.packageName)

        viewModel.dismissBlockedDialog()
        assertFalse(viewModel.uiState.value.blockedDialog.isShowing)
    }

    @Test
    fun `light focus mode triggers reflection dialog and records survey answer`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        val activeSession = FocusSession(
            id = "sess-light",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-light", title = "Reading"),
            mode = FocusMode.LIGHT,
            status = SessionStatus.ACTIVE
        )
        fakeSessionRepo.activeSession = activeSession
        fakeSessionRepo.sessionFlow.value = activeSession

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        val youtube = InstalledApp(
            packageName = "com.google.android.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            category = AppCategory.MANAGED,
            isEssential = false,
            isBlockedInFocus = true
        )

        viewModel.onAppClicked(youtube) { _, _ -> }

        assertTrue(viewModel.uiState.value.reflectionDialog.isShowing)
        assertEquals("YouTube", viewModel.uiState.value.reflectionDialog.appLabel)

        viewModel.onRecordReflection("Important / Urgent")

        assertFalse(viewModel.uiState.value.reflectionDialog.isShowing)
        assertEquals(1, fakeBlockedRepo.recorded.size)
        assertEquals("Important / Urgent", fakeBlockedRepo.recorded.first().userIntentReason)
    }

    @Test
    fun `exceeded daily limit triggers timeLimitExpiredDialog and emergency extension updates limit`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        fakeTimeLimitRepo.limits["com.google.android.youtube"] = AppTimeLimit(
            packageName = "com.google.android.youtube",
            dailyLimitMinutes = 30,
            isEnabled = true
        )
        fakeUsageRepo.usageMap["com.google.android.youtube"] = 45L // Over limit

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        val youtube = InstalledApp(
            packageName = "com.google.android.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            category = AppCategory.MANAGED,
            isEssential = false,
            isBlockedInFocus = false
        )

        viewModel.onAppClicked(youtube) { _, _ -> }

        assertTrue(viewModel.uiState.value.timeLimitExpiredDialog.isShowing)
        assertEquals("com.google.android.youtube", viewModel.uiState.value.timeLimitExpiredDialog.packageName)
        assertEquals("YouTube", viewModel.uiState.value.timeLimitExpiredDialog.appLabel)

        // User extends by +15 minutes (Total limit becomes 30 + 15 = 45)
        viewModel.extendEmergencyTime("com.google.android.youtube", 15)

        assertFalse(viewModel.uiState.value.timeLimitExpiredDialog.isShowing)
        assertEquals(15, fakeTimeLimitRepo.limits["com.google.android.youtube"]?.emergencyExtensionMinutes)
        assertEquals(45, fakeTimeLimitRepo.limits["com.google.android.youtube"]?.effectiveDailyLimitMinutes)
    }

    @Test
    fun `timeLimitExpiryNotifier event triggers timeLimitExpiredDialog automatically`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()
        val fakeTimeLimitRepo = FakeHomeTimeLimitRepository()
        val fakeUsageRepo = FakeHomeUsageStatsRepository()

        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, fakeTimeLimitRepo, fakeUsageRepo)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)
        val extendAppTimeLimitUseCase = ExtendAppTimeLimitUseCase(fakeTimeLimitRepo)
        val timeLimitExpiryNotifier = TimeLimitExpiryNotifier()

        val viewModel = HomeViewModel(
            getHomeAppsUseCase,
            syncInstalledAppsUseCase,
            launchAppUseCase,
            fakeSessionRepo,
            recordIntentReflectionUseCase,
            extendAppTimeLimitUseCase,
            timeLimitExpiryNotifier
        )

        assertFalse(viewModel.uiState.value.timeLimitExpiredDialog.isShowing)

        // Real-time notification arrives from accessibility background service
        timeLimitExpiryNotifier.notifyTimeLimitExpired("com.google.android.youtube", "YouTube")

        assertTrue(viewModel.uiState.value.timeLimitExpiredDialog.isShowing)
        assertEquals("com.google.android.youtube", viewModel.uiState.value.timeLimitExpiredDialog.packageName)
        assertEquals("YouTube", viewModel.uiState.value.timeLimitExpiredDialog.appLabel)
    }
}

