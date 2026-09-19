package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.FocusSessionDao
import com.minimalphone.core.data.db.entity.FocusGoalEntity
import com.minimalphone.core.data.db.entity.FocusSessionEntity
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultFocusSessionRepository @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) : FocusSessionRepository {

    override fun getActiveSession(): Flow<FocusSession?> {
        return focusSessionDao.observeActiveSession().map { entity ->
            entity?.toModel()?.takeIf { it.isCurrentlyActive }
        }
    }

    override suspend fun getActiveSessionSync(): FocusSession? = withContext(Dispatchers.IO) {
        val entity = focusSessionDao.getActiveSessionSync()
        entity?.toModel()?.takeIf { it.isCurrentlyActive }
    }

    override suspend fun getLatestActiveSessionRaw(): FocusSession? = withContext(Dispatchers.IO) {
        val entity = focusSessionDao.getActiveSessionSync()
        entity?.toModel()
    }

    override fun getAllSessions(): Flow<List<FocusSession>> {
        return focusSessionDao.observeAllSessions().map { list ->
            list.map { it.toModel() }
        }
    }

    override suspend fun startSession(session: FocusSession) = withContext(Dispatchers.IO) {
        val entity = FocusSessionEntity(
            id = session.id,
            startTime = session.startTime,
            endTime = session.endTime,
            durationMinutes = session.durationMinutes,
            goalId = session.goal.id,
            goalTitle = session.goal.title,
            goalCategory = session.goal.category,
            mode = session.mode,
            status = SessionStatus.ACTIVE,
            bypassAttemptsCount = session.bypassAttemptsCount,
            exitFrictionSecondsCompleted = session.exitFrictionSecondsCompleted
        )
        val goalEntity = FocusGoalEntity(
            id = session.goal.id,
            title = session.goal.title,
            category = session.goal.category,
            isCustom = session.goal.isCustom
        )
        focusSessionDao.upsertGoal(goalEntity)
        focusSessionDao.insertSession(entity)
    }

    override suspend fun completeSession(sessionId: String) = withContext(Dispatchers.IO) {
        focusSessionDao.updateSessionStatus(sessionId, SessionStatus.COMPLETED.name, 0)
    }

    override suspend fun cancelSession(sessionId: String, exitFrictionSeconds: Int, reason: String?) = withContext(Dispatchers.IO) {
        focusSessionDao.updateSessionStatus(sessionId, SessionStatus.CANCELLED_WITH_FRICTION.name, exitFrictionSeconds)
    }

    override suspend fun incrementBypassAttempt(sessionId: String) = withContext(Dispatchers.IO) {
        focusSessionDao.incrementBypassAttempt(sessionId)
    }

    override fun getRecentGoals(): Flow<List<FocusGoal>> {
        return focusSessionDao.observeRecentGoals().map { list ->
            list.map { FocusGoal(id = it.id, title = it.title, category = it.category, isCustom = it.isCustom) }
        }
    }
}

internal fun FocusSessionEntity.toModel(): FocusSession = FocusSession(
    id = id,
    startTime = startTime,
    endTime = endTime,
    durationMinutes = durationMinutes,
    goal = FocusGoal(id = goalId, title = goalTitle, category = goalCategory),
    mode = mode,
    status = status,
    bypassAttemptsCount = bypassAttemptsCount,
    exitFrictionSecondsCompleted = exitFrictionSecondsCompleted
)
