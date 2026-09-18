package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.DigestNotificationDao
import com.minimalphone.core.data.db.entity.DigestNotificationEntity
import com.minimalphone.core.model.DigestNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultNotificationDigestRepository @Inject constructor(
    private val digestDao: DigestNotificationDao
) : NotificationDigestRepository {

    override fun observeUnreadNotifications(): Flow<List<DigestNotification>> {
        return digestDao.observeUnreadNotifications().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeAllNotifications(): Flow<List<DigestNotification>> {
        return digestDao.observeAllNotifications().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeUnreadCount(): Flow<Int> {
        return digestDao.observeUnreadCount()
    }

    override suspend fun recordNotification(
        packageName: String,
        appLabel: String,
        title: String?,
        text: String?
    ) {
        digestDao.insert(
            DigestNotificationEntity(
                packageName = packageName,
                appLabel = appLabel,
                title = title,
                text = text,
                timestamp = System.currentTimeMillis(),
                isRead = false
            )
        )
    }

    override suspend fun markAllAsRead() {
        digestDao.markAllAsRead()
    }

    override suspend fun clearOldNotifications() {
        val threeDaysAgo = System.currentTimeMillis() - (3 * 86400 * 1000L)
        digestDao.clearOlderThan(threeDaysAgo)
    }

    private fun DigestNotificationEntity.toDomain() = DigestNotification(
        id = id,
        packageName = packageName,
        appLabel = appLabel,
        title = title,
        text = text,
        timestamp = timestamp,
        isRead = isRead
    )
}
