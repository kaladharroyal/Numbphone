package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.ExitAttemptDao
import com.minimalphone.core.data.db.entity.ExitAttemptEntity
import com.minimalphone.core.model.ExitAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultExitAttemptRepository @Inject constructor(
    private val exitAttemptDao: ExitAttemptDao
) : ExitAttemptRepository {

    override fun getExitAttemptsForSession(sessionId: String): Flow<List<ExitAttempt>> {
        return exitAttemptDao.observeExitAttemptsForSession(sessionId).map { list ->
            list.map {
                ExitAttempt(
                    id = it.id,
                    focusSessionId = it.focusSessionId,
                    attemptNumber = it.attemptNumber,
                    requiredFrictionSeconds = it.requiredFrictionSeconds,
                    completedFrictionSeconds = it.completedFrictionSeconds,
                    timestamp = it.timestamp,
                    isSuccessfulExit = it.isSuccessfulExit,
                    exitReason = it.exitReason
                )
            }
        }
    }

    override suspend fun getExitAttemptsCount(sessionId: String): Int = withContext(Dispatchers.IO) {
        exitAttemptDao.getExitAttemptsCount(sessionId)
    }

    override suspend fun recordExitAttempt(attempt: ExitAttempt) = withContext(Dispatchers.IO) {
        exitAttemptDao.insert(
            ExitAttemptEntity(
                focusSessionId = attempt.focusSessionId,
                attemptNumber = attempt.attemptNumber,
                requiredFrictionSeconds = attempt.requiredFrictionSeconds,
                completedFrictionSeconds = attempt.completedFrictionSeconds,
                timestamp = attempt.timestamp,
                isSuccessfulExit = attempt.isSuccessfulExit,
                exitReason = attempt.exitReason
            )
        )
    }
}
