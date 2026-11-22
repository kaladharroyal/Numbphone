package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.AppPauseConfigDao
import com.minimalphone.core.data.db.dao.PauseEventDao
import com.minimalphone.core.data.db.entity.AppPauseConfigEntity
import com.minimalphone.core.data.db.entity.PauseEventEntity
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseEvent
import com.minimalphone.core.model.PauseEventKind
import com.minimalphone.core.model.PauseOutcome
import com.minimalphone.core.model.PauseStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPauseRepository @Inject constructor(
    private val appPauseConfigDao: AppPauseConfigDao,
    private val pauseEventDao: PauseEventDao
) : PauseRepository {

    override fun observeAppPauseConfig(packageName: String): Flow<PauseConfig?> {
        return appPauseConfigDao.observe(packageName).map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun getAppPauseConfigSync(packageName: String): PauseConfig? {
        return appPauseConfigDao.get(packageName)?.toDomainModel()
    }

    override suspend fun saveAppPauseConfig(config: PauseConfig) {
        appPauseConfigDao.upsert(config.toEntity())
    }

    override fun observeAllConfigs(): Flow<List<PauseConfig>> {
        return appPauseConfigDao.observeAll().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun recordPauseEvent(event: PauseEvent) {
        pauseEventDao.insert(event.toEntity())
    }

    override fun observeTotalPausesCount(): Flow<Int> {
        return pauseEventDao.observeTotalPausesCount()
    }

    override fun observeWalkAwayCount(): Flow<Int> {
        return pauseEventDao.observeLeftCount()
    }

    override suspend fun getEventsSince(sinceTimestamp: Long): List<PauseEvent> {
        return pauseEventDao.getEventsSince(sinceTimestamp).map { it.toDomainModel() }
    }

    private fun AppPauseConfigEntity.toDomainModel(): PauseConfig = PauseConfig(
        packageName = packageName,
        enabled = enabled,
        pauseSeconds = pauseSeconds,
        style = PauseStyle.fromName(style),
        sessionMinutes = sessionMinutes,
        reinterventionEnabled = reinterventionEnabled
    )

    private fun PauseConfig.toEntity(): AppPauseConfigEntity = AppPauseConfigEntity(
        packageName = packageName,
        enabled = enabled,
        pauseSeconds = pauseSeconds,
        style = style.name,
        sessionMinutes = sessionMinutes,
        reinterventionEnabled = reinterventionEnabled
    )

    private fun PauseEventEntity.toDomainModel(): PauseEvent = PauseEvent(
        id = id,
        packageName = packageName,
        timestamp = timestamp,
        kind = PauseEventKind.fromName(kind),
        outcome = PauseOutcome.fromName(outcome),
        reflectionReason = reflectionReason,
        secondsShown = secondsShown
    )

    private fun PauseEvent.toEntity(): PauseEventEntity = PauseEventEntity(
        id = id,
        packageName = packageName,
        timestamp = timestamp,
        kind = kind.name,
        outcome = outcome.name,
        reflectionReason = reflectionReason,
        secondsShown = secondsShown
    )
}
