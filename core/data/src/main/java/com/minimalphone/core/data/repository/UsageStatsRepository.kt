package com.minimalphone.core.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.db.dao.InstalledAppDao
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppUsageStat
import com.minimalphone.core.model.DailyUsageSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

interface UsageStatsRepository {
    fun getDailyUsageSummary(): Flow<DailyUsageSummary>
    suspend fun hasUsagePermission(): Boolean
    suspend fun getTodayUsageMinutes(packageName: String): Long
}

@Singleton
class DefaultUsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppDao: InstalledAppDao
) : UsageStatsRepository {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    override suspend fun getTodayUsageMinutes(packageName: String): Long = withContext(Dispatchers.IO) {
        if (!hasUsagePermission() || usageStatsManager == null) return@withContext 0L
        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val statsList: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                calendar.timeInMillis,
                System.currentTimeMillis()
            ) ?: emptyList()

            val totalMillis = statsList
                .filter { it.packageName == packageName }
                .sumOf { it.totalTimeInForeground }
            totalMillis / (1000 * 60)
        } catch (_: Exception) {
            0L
        }
    }

    override suspend fun hasUsagePermission(): Boolean = withContext(Dispatchers.IO) {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return@withContext false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        mode == AppOpsManager.MODE_ALLOWED
    }

    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = flow {
        val hasPermission = hasUsagePermission()
        if (!hasPermission || usageStatsManager == null) {
            emit(DailyUsageSummary(hasUsagePermission = false))
            return@flow
        }

        try {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val statsList: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
            ) ?: emptyList()

            // Aggregate by package name (since queryUsageStats can return multiple intervals)
            val aggregatedStats = mutableMapOf<String, Long>()
            val lastUsedMap = mutableMapOf<String, Long>()

            for (stat in statsList) {
                if (stat.totalTimeInForeground > 0) {
                    val current = aggregatedStats.getOrDefault(stat.packageName, 0L)
                    aggregatedStats[stat.packageName] = current + stat.totalTimeInForeground
                    val lastUsed = lastUsedMap.getOrDefault(stat.packageName, 0L)
                    if (stat.lastTimeUsed > lastUsed) {
                        lastUsedMap[stat.packageName] = stat.lastTimeUsed
                    }
                }
            }

            // Exclude our own launcher from screen time stats
            aggregatedStats.remove(context.packageName)

            val appStats = mutableListOf<AppUsageStat>()
            var totalTime = 0L
            var managedTime = 0L
            var essentialTime = 0L

            for ((pkg, timeForeground) in aggregatedStats) {
                val dbApp = installedAppDao.getApp(pkg)
                val label = dbApp?.label ?: pkg.substringAfterLast('.')
                val category = dbApp?.category ?: AppCategory.MANAGED
                val isEssential = (category == AppCategory.ESSENTIAL)

                totalTime += timeForeground
                if (isEssential) {
                    essentialTime += timeForeground
                } else {
                    managedTime += timeForeground
                }

                appStats.add(
                    AppUsageStat(
                        packageName = pkg,
                        label = label,
                        totalTimeForegroundMillis = timeForeground,
                        lastTimeUsed = lastUsedMap.getOrDefault(pkg, 0L),
                        category = category,
                        isEssential = isEssential
                    )
                )
            }

            val sortedStats = appStats.sortedByDescending { it.totalTimeForegroundMillis }

            emit(
                DailyUsageSummary(
                    dateTimestamp = startOfDay,
                    totalScreenTimeMillis = totalTime,
                    managedTimeMillis = managedTime,
                    essentialTimeMillis = essentialTime,
                    appStats = sortedStats,
                    hasUsagePermission = true
                )
            )
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Error querying usage stats", e)
            emit(DailyUsageSummary(hasUsagePermission = false))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val TAG = "UsageStatsRepo"
    }
}
