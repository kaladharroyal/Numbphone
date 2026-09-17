package com.minimalphone.core.domain.boot

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBootAppRepository : AppRepository {
    var syncCalled = false

    override fun getAllApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override fun getManagedApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override fun getHomeApps(): Flow<List<InstalledApp>> = MutableStateFlow(emptyList())
    override suspend fun getApp(packageName: String): InstalledApp? = null
    override suspend fun syncInstalledApps() { syncCalled = true }
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {}
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {}
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeBootFocusSessionRepository : FocusSessionRepository {
    var activeSession: FocusSession? = null
    var completedSessionId: String? = null

    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(activeSession)
    override suspend fun getActiveSessionSync(): FocusSession? = activeSession
    override fun getAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun startSession(session: FocusSession) { activeSession = session }
    override suspend fun completeSession(sessionId: String) {
        completedSessionId = sessionId
        activeSession = null
    }
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) { activeSession = null }
    override suspend fun incrementBypassAttempt(sessionId: String) {}
    override fun getRecentGoals(): Flow<List<FocusGoal>> = MutableStateFlow(emptyList())
}

class FakeBootBlockedAttemptRepository : BlockedAttemptRepository {
    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = MutableStateFlow(emptyList())
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(5)
    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {}
}

class BootRecoveryEngineTest {

    private lateinit var appRepository: FakeBootAppRepository
    private lateinit var focusSessionRepository: FakeBootFocusSessionRepository
    private lateinit var blockedAttemptRepository: FakeBootBlockedAttemptRepository
    private lateinit var restoreFocusOnBootUseCase: RestoreFocusOnBootUseCase
    private lateinit var observeSystemDiagnosticsUseCase: ObserveSystemDiagnosticsUseCase

    @Before
    fun setUp() {
        appRepository = FakeBootAppRepository()
        focusSessionRepository = FakeBootFocusSessionRepository()
        blockedAttemptRepository = FakeBootBlockedAttemptRepository()

        restoreFocusOnBootUseCase = RestoreFocusOnBootUseCase(
            focusSessionRepository = focusSessionRepository,
            appRepository = appRepository
        )
        observeSystemDiagnosticsUseCase = ObserveSystemDiagnosticsUseCase(
            focusSessionRepository = focusSessionRepository,
            blockedAttemptRepository = blockedAttemptRepository
        )
    }

    @Test
    fun restoreFocusOnBoot_withNoActiveSession_returnsNoActiveSessionAndSyncs() = runTest {
        focusSessionRepository.activeSession = null

        val result = restoreFocusOnBootUseCase()

        assertEquals(BootRecoveryResult.NoActiveSession, result)
        assertTrue(appRepository.syncCalled)
    }

    @Test
    fun restoreFocusOnBoot_withUnexpiredSession_restoresSession() = runTest {
        val session = FocusSession(
            id = "sess_alive",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis() + 1800000, // 30 mins left
            durationMinutes = 31,
            goal = FocusGoal(id = "g1", title = "Active Coding"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        focusSessionRepository.activeSession = session

        val result = restoreFocusOnBootUseCase()

        assertTrue(result is BootRecoveryResult.Restored)
        val restored = result as BootRecoveryResult.Restored
        assertEquals("sess_alive", restored.session.id)
        assertTrue(appRepository.syncCalled)
    }

    @Test
    fun restoreFocusOnBoot_withExpiredSession_completesSessionAndReturnsExpired() = runTest {
        val expiredSession = FocusSession(
            id = "sess_expired",
            startTime = System.currentTimeMillis() - 7200000,
            endTime = System.currentTimeMillis() - 3600000, // Ended 1 hr ago
            durationMinutes = 60,
            goal = FocusGoal(id = "g2", title = "Study Completed"),
            mode = FocusMode.DEEP_FOCUS,
            status = SessionStatus.ACTIVE
        )
        focusSessionRepository.activeSession = expiredSession

        val result = restoreFocusOnBootUseCase()

        assertTrue(result is BootRecoveryResult.ExpiredAndCompleted)
        assertEquals("sess_expired", focusSessionRepository.completedSessionId)
        assertTrue(appRepository.syncCalled)
    }

    @Test
    fun observeSystemDiagnostics_combinesRepositoryAndPlatformFlags() = runTest {
        val data = observeSystemDiagnosticsUseCase(
            isDefaultLauncher = true,
            isAccessibilityEnabled = true,
            isUsageAccessGranted = true,
            isBatteryOptimizationIgnored = false,
            oemName = "Samsung",
            deviceModel = "Galaxy S24",
            androidVersion = 34,
            oemGuidance = "Disable 'Put unused apps to sleep' in Device Care."
        ).first()

        assertTrue(data.isDefaultLauncher)
        assertTrue(data.isAccessibilityEnabled)
        assertTrue(data.isUsageAccessGranted)
        assertEquals(false, data.isBatteryOptimizationIgnored)
        assertEquals(5, data.todayBlockedCount)
        assertEquals("Samsung", data.oemName)
        assertEquals("Galaxy S24", data.deviceModel)
        assertEquals("Disable 'Put unused apps to sleep' in Device Care.", data.oemGuidance)
    }
}
