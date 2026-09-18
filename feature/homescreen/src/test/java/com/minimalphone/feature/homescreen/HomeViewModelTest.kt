package com.minimalphone.feature.homescreen

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.NotificationDigestRepository
import com.minimalphone.core.domain.GetHomeAppsUseCase
import com.minimalphone.core.domain.SyncInstalledAppsUseCase
import com.minimalphone.core.domain.dumbmode.DumbModeManager
import com.minimalphone.core.domain.dumbmode.LauncherPolicy
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.friction.RecordIntentReflectionUseCase
import com.minimalphone.core.domain.launch.EvaluateAppLaunchUseCase
import com.minimalphone.core.domain.launch.LaunchAppUseCase
import com.minimalphone.core.domain.rules.RuleEngine
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(
        fakeRepo: FakeHomeAppRepository,
        fakeSessionRepo: FakeHomeFocusSessionRepository,
        fakeBlockedRepo: FakeHomeBlockedAttemptRepository,
        ruleEngine: RuleEngine = mockk(relaxed = true)
    ): HomeViewModel {
        val getHomeAppsUseCase = GetHomeAppsUseCase(fakeRepo)
        val syncInstalledAppsUseCase = SyncInstalledAppsUseCase(fakeRepo)
        val emergencyAccessManager = mockk<EmergencyAccessManager>(relaxed = true)
        coEvery { emergencyAccessManager.isEmergencyOrEssential("com.google.android.dialer") } returns true

        val evaluateAppLaunchUseCase = EvaluateAppLaunchUseCase(fakeRepo, fakeSessionRepo, emergencyAccessManager, ruleEngine)
        val launchAppUseCase = LaunchAppUseCase(evaluateAppLaunchUseCase, fakeRepo, fakeBlockedRepo)
        val recordIntentReflectionUseCase = RecordIntentReflectionUseCase(fakeBlockedRepo)

        val dumbModeManager = mockk<DumbModeManager>(relaxed = true)
        every { dumbModeManager.isDumbModeEnabled } returns MutableStateFlow(false)

        val launcherPolicy = LauncherPolicy()
        val essentialAppRepository = mockk<EssentialAppRepository>(relaxed = true)
        every { essentialAppRepository.observeEssentialPackages() } returns MutableStateFlow(emptySet())

        val contactRepository = mockk<ContactRepository>(relaxed = true)
        every { contactRepository.observePinnedContacts() } returns MutableStateFlow(emptyList())

        val digestRepository = mockk<NotificationDigestRepository>(relaxed = true)
        every { digestRepository.observeUnreadCount() } returns MutableStateFlow(0)

        return HomeViewModel(
            getHomeAppsUseCase = getHomeAppsUseCase,
            syncInstalledAppsUseCase = syncInstalledAppsUseCase,
            launchAppUseCase = launchAppUseCase,
            focusSessionRepository = fakeSessionRepo,
            recordIntentReflectionUseCase = recordIntentReflectionUseCase,
            dumbModeManager = dumbModeManager,
            launcherPolicy = launcherPolicy,
            essentialAppRepository = essentialAppRepository,
            contactRepository = contactRepository,
            digestRepository = digestRepository
        )
    }

    @Test
    fun `initial uiState observes home apps flow and focus session`() {
        val fakeRepo = FakeHomeAppRepository()
        val fakeSessionRepo = FakeHomeFocusSessionRepository()
        val fakeBlockedRepo = FakeHomeBlockedAttemptRepository()

        val viewModel = createViewModel(fakeRepo, fakeSessionRepo, fakeBlockedRepo)

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

        val viewModel = createViewModel(fakeRepo, fakeSessionRepo, fakeBlockedRepo)

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

        val ruleEngine = mockk<RuleEngine>()
        coEvery { ruleEngine.evaluate(any()) } returns AppLaunchDecision.Block(
            packageName = "com.google.android.youtube",
            reason = "Focus active",
            activeSession = activeSession
        )

        val viewModel = createViewModel(fakeRepo, fakeSessionRepo, fakeBlockedRepo, ruleEngine)

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

        val ruleEngine = mockk<RuleEngine>()
        coEvery { ruleEngine.evaluate(any()) } returns AppLaunchDecision.ShowFriction(
            packageName = "com.google.android.youtube",
            activeSession = activeSession,
            frictionSeconds = 5
        )

        val viewModel = createViewModel(fakeRepo, fakeSessionRepo, fakeBlockedRepo, ruleEngine)

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
}
