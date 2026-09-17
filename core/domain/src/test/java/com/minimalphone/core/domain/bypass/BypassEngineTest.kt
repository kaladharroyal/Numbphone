package com.minimalphone.core.domain.bypass

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.BypassDecision
import com.minimalphone.core.model.BypassRoute
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBypassAppRepository : AppRepository {
    private val apps = mutableMapOf<String, InstalledApp>()

    fun setApp(app: InstalledApp) {
        apps[app.packageName] = app
    }

    override fun getAllApps(): Flow<List<InstalledApp>> = MutableStateFlow(apps.values.toList())
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> =
        MutableStateFlow(apps.values.filter { it.isEssential || it.category == AppCategory.ESSENTIAL })
    override fun getManagedApps(): Flow<List<InstalledApp>> =
        MutableStateFlow(apps.values.filter { !it.isEssential && it.category == AppCategory.MANAGED })
    override fun getHomeApps(): Flow<List<InstalledApp>> =
        MutableStateFlow(apps.values.filter { it.isEssential })
    override suspend fun getApp(packageName: String): InstalledApp? = apps[packageName]
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {}
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {}
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeBypassFocusSessionRepository : FocusSessionRepository {
    var activeSession: FocusSession? = null
    var bypassCount = 0

    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(activeSession)
    override suspend fun getActiveSessionSync(): FocusSession? = activeSession
    override fun getAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun startSession(session: FocusSession) { activeSession = session }
    override suspend fun completeSession(sessionId: String) { activeSession = null }
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) { activeSession = null }
    override suspend fun incrementBypassAttempt(sessionId: String) { bypassCount++ }
    override fun getRecentGoals(): Flow<List<FocusGoal>> = MutableStateFlow(emptyList())
}

class FakeBypassBlockedAttemptRepository : BlockedAttemptRepository {
    val recorded = mutableListOf<BlockedAttempt>()

    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = MutableStateFlow(recorded)
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(recorded.size)
    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {
        recorded.add(attempt)
    }
}

class BypassEngineTest {

    private lateinit var appRepository: FakeBypassAppRepository
    private lateinit var focusSessionRepository: FakeBypassFocusSessionRepository
    private lateinit var blockedAttemptRepository: FakeBypassBlockedAttemptRepository
    private lateinit var evaluateBypassRouteUseCase: EvaluateBypassRouteUseCase
    private lateinit var recordBypassAttemptUseCase: RecordBypassAttemptUseCase

    @Before
    fun setUp() {
        appRepository = FakeBypassAppRepository()
        focusSessionRepository = FakeBypassFocusSessionRepository()
        blockedAttemptRepository = FakeBypassBlockedAttemptRepository()

        evaluateBypassRouteUseCase = EvaluateBypassRouteUseCase(appRepository, focusSessionRepository)
        recordBypassAttemptUseCase = RecordBypassAttemptUseCase(
            appRepository = appRepository,
            blockedAttemptRepository = blockedAttemptRepository,
            focusSessionRepository = focusSessionRepository
        )

        // Seed sample apps
        appRepository.setApp(
            InstalledApp(
                packageName = "com.google.android.dialer",
                label = "Phone",
                activityName = "com.google.android.dialer.DialtactsActivity",
                category = AppCategory.ESSENTIAL,
                isEssential = true
            )
        )
        appRepository.setApp(
            InstalledApp(
                packageName = "com.instagram.android",
                label = "Instagram",
                activityName = "com.instagram.mainactivity.MainActivity",
                category = AppCategory.MANAGED,
                isEssential = false
            )
        )
    }

    @Test
    fun evaluateBypass_allowsMinimalPhoneItself() = runTest {
        val decision = evaluateBypassRouteUseCase("com.minimalphone")
        assertEquals(BypassDecision.Allow, decision)
    }

    @Test
    fun evaluateBypass_allowsSystemUIAndFramework() = runTest {
        assertEquals(BypassDecision.Allow, evaluateBypassRouteUseCase("android"))
        assertEquals(BypassDecision.Allow, evaluateBypassRouteUseCase("com.android.systemui"))
        assertEquals(BypassDecision.Allow, evaluateBypassRouteUseCase("com.android.settings"))
    }

    @Test
    fun evaluateBypass_allowsEssentialAppsAlways() = runTest {
        // Even during active deep focus session
        focusSessionRepository.activeSession = FocusSession(
            id = "sess_1",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 3600000,
            durationMinutes = 60,
            goal = FocusGoal(id = "g1", title = "Study Session"),
            mode = FocusMode.DEEP_FOCUS,
            status = SessionStatus.ACTIVE
        )

        val decision = evaluateBypassRouteUseCase("com.google.android.dialer")
        assertEquals(BypassDecision.Allow, decision)
    }

    @Test
    fun evaluateBypass_allowsManagedAppsWhenNoSessionActive() = runTest {
        focusSessionRepository.activeSession = null
        val decision = evaluateBypassRouteUseCase("com.instagram.android")
        assertEquals(BypassDecision.Allow, decision)
    }

    @Test
    fun evaluateBypass_allowsManagedAppsInLightMode() = runTest {
        focusSessionRepository.activeSession = FocusSession(
            id = "sess_light",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 3600000,
            durationMinutes = 60,
            goal = FocusGoal(id = "g1", title = "Light Focus"),
            mode = FocusMode.LIGHT,
            status = SessionStatus.ACTIVE
        )

        val decision = evaluateBypassRouteUseCase("com.instagram.android")
        assertEquals(BypassDecision.Allow, decision)
    }

    @Test
    fun evaluateBypass_interceptsManagedAppsInStrictMode() = runTest {
        val session = FocusSession(
            id = "sess_strict",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 3600000,
            durationMinutes = 60,
            goal = FocusGoal(id = "g1", title = "Strict Study"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        focusSessionRepository.activeSession = session

        val decision = evaluateBypassRouteUseCase("com.instagram.android")
        assertTrue(decision is BypassDecision.InterceptAndRedirect)
        val intercept = decision as BypassDecision.InterceptAndRedirect
        assertEquals("com.instagram.android", intercept.packageName)
        assertEquals("sess_strict", intercept.session.id)
    }

    @Test
    fun evaluateBypass_interceptsManagedAppsInDeepFocusMode() = runTest {
        val session = FocusSession(
            id = "sess_deep",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 3600000,
            durationMinutes = 60,
            goal = FocusGoal(id = "g1", title = "Deep Coding"),
            mode = FocusMode.DEEP_FOCUS,
            status = SessionStatus.ACTIVE
        )
        focusSessionRepository.activeSession = session

        val decision = evaluateBypassRouteUseCase("com.instagram.android")
        assertTrue(decision is BypassDecision.InterceptAndRedirect)
    }

    @Test
    fun recordBypassAttempt_logsToRepositoryAndIncrementsSessionBypassCount() = runTest {
        val session = FocusSession(
            id = "sess_bypass_test",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 3600000,
            durationMinutes = 60,
            goal = FocusGoal(id = "g1", title = "Exam Prep"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        focusSessionRepository.activeSession = session

        recordBypassAttemptUseCase(
            packageName = "com.instagram.android",
            route = BypassRoute.NOTIFICATION
        )

        assertEquals(1, blockedAttemptRepository.recorded.size)
        val attempt = blockedAttemptRepository.recorded.first()
        assertEquals("com.instagram.android", attempt.packageName)
        assertEquals("Instagram", attempt.appLabel)
        assertEquals("NOTIFICATION", attempt.route)
        assertEquals("sess_bypass_test", attempt.focusSessionId)
        assertEquals(1, focusSessionRepository.bypassCount)
    }
}
