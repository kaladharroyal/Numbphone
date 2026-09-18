package com.minimalphone.feature.screentime

import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.domain.insights.GetBehavioralInsightsUseCase
import com.minimalphone.core.domain.insights.GetWeeklyWellbeingReportUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppUsageStat
import com.minimalphone.core.model.BlockedAttempt
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeUsageStatsRepository : UsageStatsRepository {
    val summaryFlow = MutableStateFlow(
        DailyUsageSummary(
            dateTimestamp = 1000L,
            totalScreenTimeMillis = 3600000L, // 1 hour
            managedTimeMillis = 2400000L, // 40 min
            essentialTimeMillis = 1200000L, // 20 min
            appStats = listOf(
                AppUsageStat(
                    packageName = "com.google.android.youtube",
                    label = "YouTube",
                    totalTimeForegroundMillis = 2400000L,
                    category = AppCategory.MANAGED,
                    isEssential = false
                ),
                AppUsageStat(
                    packageName = "com.google.android.dialer",
                    label = "Phone",
                    totalTimeForegroundMillis = 1200000L,
                    category = AppCategory.ESSENTIAL,
                    isEssential = true
                )
            ),
            hasUsagePermission = true
        )
    )

    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = summaryFlow.asStateFlow()
    override suspend fun hasUsagePermission(): Boolean = summaryFlow.value.hasUsagePermission
}

class FakeBlockedAttemptRepo : BlockedAttemptRepository {
    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> = MutableStateFlow(emptyList())
    override fun getTodayBlockedCount(): Flow<Int> = MutableStateFlow(0)
    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) {}
}

class FakeFocusSessionRepo : FocusSessionRepository {
    override fun getActiveSession(): Flow<FocusSession?> = MutableStateFlow(null)
    override suspend fun getActiveSessionSync(): FocusSession? = null
    override fun getAllSessions(): Flow<List<FocusSession>> = MutableStateFlow(emptyList())
    override suspend fun startSession(session: FocusSession) {}
    override suspend fun completeSession(sessionId: String) {}
    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) {}
    override suspend fun incrementBypassAttempt(sessionId: String) {}
    override fun getRecentGoals(): Flow<List<FocusGoal>> = MutableStateFlow(emptyList())
}

class ScreenTimeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadStats populates screen time summary correctly`() {
        val fakeRepo = FakeUsageStatsRepository()
        val fakeBlocked = FakeBlockedAttemptRepo()
        val fakeFocus = FakeFocusSessionRepo()

        val usageUseCase = GetDailyUsageStatsUseCase(fakeRepo)
        val insightsUseCase = GetBehavioralInsightsUseCase(fakeBlocked, fakeFocus, fakeRepo)
        val reportUseCase = GetWeeklyWellbeingReportUseCase(fakeBlocked, fakeFocus, fakeRepo)

        val viewModel = ScreenTimeViewModel(usageUseCase, insightsUseCase, reportUseCase)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.summary.hasUsagePermission)
        assertEquals(2, state.summary.appStats.size)
        assertEquals("1h 0m", state.summary.formattedTotalTime)
        assertEquals("YouTube", state.summary.appStats.first().label)
    }

    @Test
    fun `formattedDuration formats hours and minutes properly`() {
        val stat = AppUsageStat(
            packageName = "com.test",
            label = "Test",
            totalTimeForegroundMillis = 3720000L // 1h 2m
        )
        assertEquals("1h 2m", stat.formattedDuration)
    }
}
