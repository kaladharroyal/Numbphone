package com.minimalphone.feature.screentime

import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.GetAppTimeLimitsUseCase
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.domain.RemoveAppTimeLimitUseCase
import com.minimalphone.core.domain.SetAppTimeLimitUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.AppUsageStat
import com.minimalphone.core.model.DailyUsageSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeTimeLimitRepo : AppTimeLimitRepository {
    private val limits = MutableStateFlow<List<AppTimeLimit>>(emptyList())
    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = limits.asStateFlow()
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits.value.find { it.packageName == packageName }
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(null)
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {}
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {}
    override suspend fun removeLimit(packageName: String) {}
}

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
    override suspend fun getTodayUsageMinutes(packageName: String): Long = 40L
}

class ScreenTimeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadStats populates screen time summary correctly`() {
        val fakeRepo = FakeUsageStatsRepository()
        val fakeLimitRepo = FakeTimeLimitRepo()
        val usageUseCase = GetDailyUsageStatsUseCase(fakeRepo)
        val getLimitsUseCase = GetAppTimeLimitsUseCase(fakeLimitRepo)
        val setLimitUseCase = SetAppTimeLimitUseCase(fakeLimitRepo)
        val removeLimitUseCase = RemoveAppTimeLimitUseCase(fakeLimitRepo)

        val viewModel = ScreenTimeViewModel(
            usageUseCase,
            getLimitsUseCase,
            setLimitUseCase,
            removeLimitUseCase
        )

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
