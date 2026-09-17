package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.DailyUsageSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetDailyUsageStatsUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository
) {
    operator fun invoke(): Flow<DailyUsageSummary> {
        return usageStatsRepository.getDailyUsageSummary()
    }
}

class CheckUsagePermissionUseCase @Inject constructor(
    private val usageStatsRepository: UsageStatsRepository
) {
    suspend operator fun invoke(): Boolean {
        return usageStatsRepository.hasUsagePermission()
    }
}
