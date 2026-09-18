package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.AppBudgetDao
import com.minimalphone.core.data.db.dao.DailyAppUsageDao
import com.minimalphone.core.data.db.entity.AppBudgetEntity
import com.minimalphone.core.data.db.entity.DailyAppUsageEntity
import com.minimalphone.core.model.AppBudget
import com.minimalphone.core.model.DailyAppUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBudgetRepository @Inject constructor(
    private val appBudgetDao: AppBudgetDao,
    private val dailyAppUsageDao: DailyAppUsageDao
) : BudgetRepository {

    override fun observeAllBudgets(): Flow<List<AppBudget>> =
        appBudgetDao.observeAllBudgets().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun getBudget(packageName: String): AppBudget? =
        appBudgetDao.getBudget(packageName)?.toDomain()

    override suspend fun setBudget(packageName: String, appLabel: String, dailyLimitMinutes: Int) {
        appBudgetDao.upsert(
            AppBudgetEntity(
                packageName = packageName,
                dailyLimitMinutes = dailyLimitMinutes,
                enabled = dailyLimitMinutes > 0
            )
        )
    }

    override suspend fun removeBudget(packageName: String) {
        appBudgetDao.delete(packageName)
    }

    override suspend fun getTodayUsageMinutes(packageName: String): Int {
        val today = LocalDate.now().toString()
        return dailyAppUsageDao.getUsage(packageName, today)?.foregroundMinutes ?: 0
    }

    override suspend fun recordUsage(
        packageName: String,
        date: String,
        foregroundMinutes: Int,
        launchCount: Int
    ) {
        val existing = dailyAppUsageDao.getUsage(packageName, date)
        dailyAppUsageDao.upsert(
            DailyAppUsageEntity(
                packageName = packageName,
                date = date,
                foregroundMinutes = foregroundMinutes,
                launchCount = launchCount,
                blockedAttempts = existing?.blockedAttempts ?: 0
            )
        )
    }

    override suspend fun incrementBlockedAttempt(packageName: String, date: String) {
        dailyAppUsageDao.incrementBlockedAttempts(packageName, date)
    }

    override fun observeTodayUsage(date: String): Flow<List<DailyAppUsage>> =
        dailyAppUsageDao.observeUsageForDate(date).map { list ->
            list.map { it.toDomain() }
        }

    private fun AppBudgetEntity.toDomain() = AppBudget(
        packageName = packageName,
        dailyLimitMinutes = dailyLimitMinutes,
        enabled = enabled
    )

    private fun DailyAppUsageEntity.toDomain() = DailyAppUsage(
        packageName = packageName,
        date = date,
        foregroundMinutes = foregroundMinutes,
        launchCount = launchCount,
        blockedAttempts = blockedAttempts
    )
}
