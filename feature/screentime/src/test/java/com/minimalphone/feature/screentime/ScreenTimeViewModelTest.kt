package com.minimalphone.feature.screentime

import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.model.AppCategory
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

class ScreenTimeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadStats populates screen time summary correctly`() {
        val fakeRepo = FakeUsageStatsRepository()
        val useCase = GetDailyUsageStatsUseCase(fakeRepo)
        val viewModel = ScreenTimeViewModel(useCase)

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
