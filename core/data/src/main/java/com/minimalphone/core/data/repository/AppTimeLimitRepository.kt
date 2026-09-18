package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.AppTimeLimitDao
import com.minimalphone.core.data.db.entity.AppTimeLimitEntity
import com.minimalphone.core.model.AppTimeLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AppTimeLimitRepository {
    fun observeAllLimits(): Flow<List<AppTimeLimit>>
    suspend fun getLimit(packageName: String): AppTimeLimit?
    fun observeLimit(packageName: String): Flow<AppTimeLimit?>
    suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean = true)
    suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int)
    suspend fun removeLimit(packageName: String)
}

@Singleton
class DefaultAppTimeLimitRepository @Inject constructor(
    private val appTimeLimitDao: AppTimeLimitDao
) : AppTimeLimitRepository {

    private fun getStartOfDayMillis(): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    override fun observeAllLimits(): Flow<List<AppTimeLimit>> {
        val startOfDay = getStartOfDayMillis()
        return appTimeLimitDao.observeAllLimits().map { entities ->
            entities.map { it.toModel(startOfDay) }
        }
    }

    override suspend fun getLimit(packageName: String): AppTimeLimit? {
        val startOfDay = getStartOfDayMillis()
        return appTimeLimitDao.getLimit(packageName)?.toModel(startOfDay)
    }

    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> {
        val startOfDay = getStartOfDayMillis()
        return appTimeLimitDao.observeLimit(packageName).map { it?.toModel(startOfDay) }
    }

    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        val existing = appTimeLimitDao.getLimit(packageName)
        appTimeLimitDao.upsertLimit(
            AppTimeLimitEntity(
                packageName = packageName,
                dailyLimitMinutes = limitMinutes,
                isEnabled = isEnabled,
                updatedTimestamp = System.currentTimeMillis(),
                emergencyExtensionMinutes = existing?.emergencyExtensionMinutes ?: 0,
                lastExtensionDateMillis = existing?.lastExtensionDateMillis ?: 0L
            )
        )
    }

    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDayMillis()
        val existing = appTimeLimitDao.getLimit(packageName)
        if (existing != null) {
            val currentExtension = if (existing.lastExtensionDateMillis >= startOfDay) {
                existing.emergencyExtensionMinutes
            } else {
                0
            }
            appTimeLimitDao.upsertLimit(
                existing.copy(
                    emergencyExtensionMinutes = currentExtension + additionalMinutes,
                    lastExtensionDateMillis = now,
                    updatedTimestamp = now
                )
            )
        }
    }

    override suspend fun removeLimit(packageName: String) {
        appTimeLimitDao.deleteLimit(packageName)
    }
}

private fun AppTimeLimitEntity.toModel(startOfDayMillis: Long = 0L): AppTimeLimit {
    val effectiveExtension = if (lastExtensionDateMillis >= startOfDayMillis) {
        emergencyExtensionMinutes
    } else {
        0
    }
    return AppTimeLimit(
        packageName = packageName,
        dailyLimitMinutes = dailyLimitMinutes,
        isEnabled = isEnabled,
        updatedTimestamp = updatedTimestamp,
        emergencyExtensionMinutes = effectiveExtension,
        lastExtensionDateMillis = lastExtensionDateMillis
    )
}
