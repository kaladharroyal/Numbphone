package com.minimalphone.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.domain.insights.GetBehavioralInsightsUseCase
import com.minimalphone.core.domain.insights.GetWeeklyWellbeingReportUseCase
import com.minimalphone.core.model.BehavioralInsights
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.WeeklyWellbeingReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenTimeUiState(
    val summary: DailyUsageSummary = DailyUsageSummary(),
    val insights: BehavioralInsights = BehavioralInsights(),
    val weeklyReport: WeeklyWellbeingReport = WeeklyWellbeingReport(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val getDailyUsageStatsUseCase: GetDailyUsageStatsUseCase,
    private val getBehavioralInsightsUseCase: GetBehavioralInsightsUseCase,
    private val getWeeklyWellbeingReportUseCase: GetWeeklyWellbeingReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenTimeUiState())
    val uiState: StateFlow<ScreenTimeUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            combine(
                getDailyUsageStatsUseCase(),
                getBehavioralInsightsUseCase(),
                getWeeklyWellbeingReportUseCase()
            ) { usage, insights, report ->
                ScreenTimeUiState(
                    summary = usage,
                    insights = insights,
                    weeklyReport = report,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
