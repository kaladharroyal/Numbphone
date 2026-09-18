package com.minimalphone.feature.appslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.GetAppTimeLimitsUseCase
import com.minimalphone.core.domain.RemoveAppTimeLimitUseCase
import com.minimalphone.core.domain.SetAppTimeLimitUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.InstalledApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppListFilter {
    ALL,
    HOME,
    ESSENTIAL,
    MANAGED
}

data class AppsManagementUiState(
    val searchQuery: String = "",
    val activeFilter: AppListFilter = AppListFilter.ALL,
    val apps: List<InstalledApp> = emptyList(),
    val totalAppsCount: Int = 0,
    val homeAppsCount: Int = 0,
    val essentialCount: Int = 0,
    val managedCount: Int = 0,
    val limitsMap: Map<String, AppTimeLimit> = emptyMap(),
    val usageMinutesMap: Map<String, Long> = emptyMap()
)

@HiltViewModel
class AppsManagementViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val getAppTimeLimitsUseCase: GetAppTimeLimitsUseCase,
    private val setAppTimeLimitUseCase: SetAppTimeLimitUseCase,
    private val removeAppTimeLimitUseCase: RemoveAppTimeLimitUseCase,
    private val usageStatsRepository: UsageStatsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(AppListFilter.ALL)

    val uiState: StateFlow<AppsManagementUiState> = combine(
        appRepository.getAllApps(),
        _searchQuery,
        _activeFilter,
        getAppTimeLimitsUseCase(),
        usageStatsRepository.getDailyUsageSummary()
    ) { allApps, query, filter, limits, usageSummary ->
        val filtered = allApps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppListFilter.ALL -> true
                AppListFilter.HOME -> app.isFavoriteOnHome
                AppListFilter.ESSENTIAL -> app.category == AppCategory.ESSENTIAL
                AppListFilter.MANAGED -> app.category == AppCategory.MANAGED
            }

            matchesQuery && matchesFilter
        }

        val limitsMap = limits.associateBy { it.packageName }
        val usageMap = usageSummary.appStats.associate { it.packageName to it.totalMinutes }

        AppsManagementUiState(
            searchQuery = query,
            activeFilter = filter,
            apps = filtered,
            totalAppsCount = allApps.size,
            homeAppsCount = allApps.count { it.isFavoriteOnHome },
            essentialCount = allApps.count { it.category == AppCategory.ESSENTIAL },
            managedCount = allApps.count { it.category == AppCategory.MANAGED },
            limitsMap = limitsMap,
            usageMinutesMap = usageMap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppsManagementUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(filter: AppListFilter) {
        _activeFilter.value = filter
    }

    fun toggleAppCategory(app: InstalledApp) {
        viewModelScope.launch {
            val newCategory = if (app.category == AppCategory.ESSENTIAL) {
                AppCategory.MANAGED
            } else {
                AppCategory.ESSENTIAL
            }
            appRepository.updateAppCategory(app.packageName, newCategory)
        }
    }

    fun toggleFavoriteOnHome(app: InstalledApp) {
        viewModelScope.launch {
            appRepository.toggleFavoriteOnHome(app.packageName, !app.isFavoriteOnHome)
        }
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

    fun refreshApps() {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }
}
