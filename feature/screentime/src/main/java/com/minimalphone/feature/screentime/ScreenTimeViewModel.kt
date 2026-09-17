package com.minimalphone.feature.screentime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.domain.GetDailyUsageStatsUseCase
import com.minimalphone.core.model.DailyUsageSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScreenTimeUiState(
    val summary: DailyUsageSummary = DailyUsageSummary(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    private val getDailyUsageStatsUseCase: GetDailyUsageStatsUseCase
) : ViewModel() {

    private val _refreshTrigger = MutableStateFlow(0)

    val uiState: StateFlow<ScreenTimeUiState> = MutableStateFlow(ScreenTimeUiState())

    init {
        loadStats()
    }

    fun loadStats() {
        viewModelScope.launch {
            getDailyUsageStatsUseCase().collect { summary ->
                (uiState as MutableStateFlow).value = ScreenTimeUiState(
                    summary = summary,
                    isLoading = false
                )
            }
        }
    }
}
