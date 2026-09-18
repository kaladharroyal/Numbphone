package com.minimalphone.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.domain.GetAppTimeLimitsUseCase
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.domain.RemoveAppTimeLimitUseCase
import com.minimalphone.core.domain.SetAppTimeLimitUseCase
import com.minimalphone.core.domain.insights.GetBehavioralInsightsUseCase
import com.minimalphone.core.domain.insights.GetWeeklyWellbeingReportUseCase
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.BehavioralInsights
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.WeeklyWellbeingReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenTimeUiState(
    val summary: DailyUsageSummary = DailyUsageSummary(),
    val insights: BehavioralInsights = BehavioralInsights(),
    val weeklyReport: WeeklyWellbeingReport = WeeklyWellbeingReport(),
    val limitsMap: Map<String, AppTimeLimit> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val getDailyUsageStatsUseCase: GetDailyUsageStatsUseCase,
    private val getBehavioralInsightsUseCase: GetBehavioralInsightsUseCase,
    private val getWeeklyWellbeingReportUseCase: GetWeeklyWellbeingReportUseCase,
    private val getAppTimeLimitsUseCase: GetAppTimeLimitsUseCase,
    private val setAppTimeLimitUseCase: SetAppTimeLimitUseCase,
    private val removeAppTimeLimitUseCase: RemoveAppTimeLimitUseCase
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ScreenTimeUiState> = combine(
        getDailyUsageStatsUseCase(),
        getBehavioralInsightsUseCase(),
        getWeeklyWellbeingReportUseCase(),
        getAppTimeLimitsUseCase(),
        _refreshTrigger
    ) { usage, insights, report, limits, _ ->
        val map = limits.associateBy { it.packageName }
        ScreenTimeUiState(
            summary = usage,
            insights = insights,
            weeklyReport = report,
            limitsMap = map,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ScreenTimeUiState()
    )

    fun loadStats() {
        _refreshTrigger.value += 1
    }

    fun setAppTimeLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            setAppTimeLimitUseCase(packageName, limitMinutes)
        }
    }

    fun removeAppTimeLimit(packageName: String) {
        viewModelScope.launch {
            removeAppTimeLimitUseCase(packageName)
        }
    }
}
