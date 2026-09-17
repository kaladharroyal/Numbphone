package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.BlockedAttemptDao
import com.minimalphone.core.data.db.entity.BlockedAttemptEntity
import com.minimalphone.core.model.BlockedAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBlockedAttemptRepository @Inject constructor(
    private val blockedAttemptDao: BlockedAttemptDao
) : BlockedAttemptRepository {

    override fun getBlockedAttempts(): Flow<List<BlockedAttempt>> {
        return blockedAttemptDao.observeAllBlocked().map { list ->
            list.map { it.toModel() }
        }
    }

    override fun getTodayBlockedCount(): Flow<Int> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return blockedAttemptDao.observeBlockedCountSince(calendar.timeInMillis)
    }

    override suspend fun recordBlockedAttempt(attempt: BlockedAttempt) = withContext(Dispatchers.IO) {
        val entity = BlockedAttemptEntity(
            packageName = attempt.packageName,
            appLabel = attempt.appLabel,
            timestamp = attempt.timestamp,
            focusSessionId = attempt.focusSessionId,
            route = attempt.route,
            userIntentReason = attempt.userIntentReason
        )
        blockedAttemptDao.insert(entity)
    }
}

internal fun BlockedAttemptEntity.toModel(): BlockedAttempt = BlockedAttempt(
    id = id,
    packageName = packageName,
    appLabel = appLabel,
    timestamp = timestamp,
    focusSessionId = focusSessionId,
    route = route,
    userIntentReason = userIntentReason
)
