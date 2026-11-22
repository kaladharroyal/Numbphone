package com.minimalphone.feature.appslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.quarantine.QuarantineEngine
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
    MANAGED,
    QUARANTINED
}

data class AppsManagementUiState(
    val searchQuery: String = "",
    val activeFilter: AppListFilter = AppListFilter.ALL,
    val apps: List<InstalledApp> = emptyList(),
    val totalAppsCount: Int = 0,
    val homeAppsCount: Int = 0,
    val essentialCount: Int = 0,
    val managedCount: Int = 0,
    val quarantinedCount: Int = 0,
    val limitsMap: Map<String, AppTimeLimit> = emptyMap(),
    val usageMinutesMap: Map<String, Long> = emptyMap()
)

@HiltViewModel
class AppsManagementViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val quarantineEngine: QuarantineEngine = QuarantineEngine()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(AppListFilter.ALL)

    val uiState: StateFlow<AppsManagementUiState> = combine(
        appRepository.getAllApps(),
        _searchQuery,
        _activeFilter,
        appTimeLimitRepository.observeAllLimits(),
        usageStatsRepository.getDailyUsageSummary()
    ) { allApps, query, filter, limits, usageSummary ->
        val now = System.currentTimeMillis()
        val filtered = allApps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppListFilter.ALL -> true
                AppListFilter.HOME -> app.isFavoriteOnHome
                AppListFilter.ESSENTIAL -> app.category == AppCategory.ESSENTIAL
                AppListFilter.MANAGED -> app.category == AppCategory.MANAGED
                AppListFilter.QUARANTINED -> quarantineEngine.isQuarantined(app.quarantineUntil, now)
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
            quarantinedCount = allApps.count { quarantineEngine.isQuarantined(it.quarantineUntil, now) },
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

    fun onSearchQueryChange(query: String) = onSearchQueryChanged(query)

    fun onFilterSelected(filter: AppListFilter) {
        _activeFilter.value = filter
    }

    fun onFilterSelect(filter: AppListFilter) = onFilterSelected(filter)

    fun refreshApps() {
        viewModelScope.launch {
            appRepository.syncInstalledApps()
        }
    }

    fun toggleAppCategory(app: InstalledApp) {
        toggleAppCategory(app.packageName, app.category)
    }

    fun toggleAppCategory(packageName: String, currentCategory: AppCategory) {
        viewModelScope.launch {
            val newCategory = if (currentCategory == AppCategory.ESSENTIAL) {
                AppCategory.MANAGED
            } else {
                AppCategory.ESSENTIAL
            }
            appRepository.updateAppCategory(packageName, newCategory)
        }
    }

    fun toggleFavoriteOnHome(app: InstalledApp) {
        toggleFavoriteOnHome(app.packageName, app.isFavoriteOnHome)
    }

    fun toggleFavoriteOnHome(packageName: String, currentFavorite: Boolean) {
        viewModelScope.launch {
            appRepository.toggleFavoriteOnHome(packageName, !currentFavorite)
        }
    }

    fun setAppTimeLimit(packageName: String, limitMinutes: Int) {
        viewModelScope.launch {
            appTimeLimitRepository.setLimit(packageName, limitMinutes)
        }
    }

    fun removeAppTimeLimit(packageName: String) {
        viewModelScope.launch {
            appTimeLimitRepository.removeLimit(packageName)
        }
    }

    fun formatRemainingQuarantine(quarantineUntil: Long?): String {
        return quarantineEngine.remainingHours(quarantineUntil, System.currentTimeMillis())
    }
}
