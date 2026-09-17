package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.InstalledApp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke(): Flow<List<InstalledApp>> {
        return appRepository.getAllApps()
    }
}

class GetHomeAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    operator fun invoke(): Flow<List<InstalledApp>> {
        return appRepository.getHomeApps()
    }
}

class SyncInstalledAppsUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke() {
        appRepository.syncInstalledApps()
    }
}

class UpdateAppCategoryUseCase @Inject constructor(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageName: String, category: AppCategory) {
        appRepository.updateAppCategory(packageName, category)
    }
}
