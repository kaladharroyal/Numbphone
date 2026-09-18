package com.minimalphone.feature.appslist

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.domain.GetAppTimeLimitsUseCase
import com.minimalphone.core.domain.RemoveAppTimeLimitUseCase
import com.minimalphone.core.domain.SetAppTimeLimitUseCase
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FakeManagementAppRepository : AppRepository {
    val appsFlow = MutableStateFlow(
        listOf(
            InstalledApp(
                packageName = "com.google.android.dialer",
                activityName = "DialerActivity",
                label = "Phone",
                category = AppCategory.ESSENTIAL,
                isEssential = true,
                isBlockedInFocus = false,
                isFavoriteOnHome = true
            ),
            InstalledApp(
                packageName = "com.google.android.youtube",
                activityName = "YouTubeActivity",
                label = "YouTube",
                category = AppCategory.MANAGED,
                isEssential = false,
                isBlockedInFocus = true,
                isFavoriteOnHome = false
            ),
            InstalledApp(
                packageName = "com.instagram.android",
                activityName = "InstagramActivity",
                label = "Instagram",
                category = AppCategory.MANAGED,
                isEssential = false,
                isBlockedInFocus = true,
                isFavoriteOnHome = false
            )
        )
    )

    override fun getAllApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getManagedApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override fun getHomeApps(): Flow<List<InstalledApp>> = appsFlow.asStateFlow()
    override suspend fun getApp(packageName: String): InstalledApp? = appsFlow.value.find { it.packageName == packageName }
    override suspend fun syncInstalledApps() {}
    override suspend fun updateAppCategory(packageName: String, category: AppCategory) {
        appsFlow.value = appsFlow.value.map {
            if (it.packageName == packageName) it.copy(category = category, isEssential = (category == AppCategory.ESSENTIAL)) else it
        }
    }
    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) {
        appsFlow.value = appsFlow.value.map {
            if (it.packageName == packageName) it.copy(isFavoriteOnHome = isFavorite) else it
        }
    }
    override suspend fun onPackageAdded(packageName: String) {}
    override suspend fun onPackageRemoved(packageName: String) {}
    override suspend fun onPackageChanged(packageName: String) {}
}

class FakeManagementTimeLimitRepo : AppTimeLimitRepository {
    private val limits = MutableStateFlow<List<AppTimeLimit>>(emptyList())
    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = limits.asStateFlow()
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits.value.find { it.packageName == packageName }
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(null)
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {}
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {}
    override suspend fun removeLimit(packageName: String) {}
}

class FakeManagementUsageStatsRepo : UsageStatsRepository {
    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = MutableStateFlow(DailyUsageSummary())
    override suspend fun hasUsagePermission(): Boolean = true
    override suspend fun getTodayUsageMinutes(packageName: String): Long = 0L
}

class AppsManagementViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(repo: FakeManagementAppRepository): AppsManagementViewModel {
        val limitRepo = FakeManagementTimeLimitRepo()
        val usageRepo = FakeManagementUsageStatsRepo()
        return AppsManagementViewModel(
            appRepository = repo,
            getAppTimeLimitsUseCase = GetAppTimeLimitsUseCase(limitRepo),
            setAppTimeLimitUseCase = SetAppTimeLimitUseCase(limitRepo),
            removeAppTimeLimitUseCase = RemoveAppTimeLimitUseCase(limitRepo),
            usageStatsRepository = usageRepo
        )
    }

    @Test
    fun `search filter filters apps list correctly`() {
        val repo = FakeManagementAppRepository()
        val viewModel = createViewModel(repo)

        assertEquals(3, viewModel.uiState.value.apps.size)

        viewModel.onSearchQueryChanged("You")
        assertEquals(1, viewModel.uiState.value.apps.size)
        assertEquals("YouTube", viewModel.uiState.value.apps.first().label)
    }

    @Test
    fun `category filter filters essential and managed apps`() {
        val repo = FakeManagementAppRepository()
        val viewModel = createViewModel(repo)

        viewModel.onFilterSelected(AppListFilter.ESSENTIAL)
        assertEquals(1, viewModel.uiState.value.apps.size)
        assertEquals("Phone", viewModel.uiState.value.apps.first().label)

        viewModel.onFilterSelected(AppListFilter.MANAGED)
        assertEquals(2, viewModel.uiState.value.apps.size)

        viewModel.onFilterSelected(AppListFilter.HOME)
        assertEquals(1, viewModel.uiState.value.apps.size)
        assertEquals("Phone", viewModel.uiState.value.apps.first().label)
        assertEquals(1, viewModel.uiState.value.homeAppsCount)
    }

    @Test
    fun `toggleFavoriteOnHome updates home apps filter dynamically`() {
        val repo = FakeManagementAppRepository()
        val viewModel = createViewModel(repo)

        viewModel.onFilterSelected(AppListFilter.HOME)
        assertEquals(1, viewModel.uiState.value.apps.size)

        val youtube = repo.appsFlow.value.first { it.packageName == "com.google.android.youtube" }
        viewModel.toggleFavoriteOnHome(youtube)

        assertEquals(2, viewModel.uiState.value.apps.size)
        assertEquals(2, viewModel.uiState.value.homeAppsCount)
    }

    @Test
    fun `toggleAppCategory switches category and updates state`() {
        val repo = FakeManagementAppRepository()
        val viewModel = createViewModel(repo)

        val youtube = repo.appsFlow.value.first { it.packageName == "com.google.android.youtube" }
        viewModel.toggleAppCategory(youtube)

        val updated = repo.appsFlow.value.first { it.packageName == "com.google.android.youtube" }
        assertEquals(AppCategory.ESSENTIAL, updated.category)
        assertTrue(updated.isEssential)
    }
}
