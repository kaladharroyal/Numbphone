package com.minimalphone.core.data.repository

import com.minimalphone.core.data.db.dao.FocusPresetDao
import com.minimalphone.core.data.db.dao.FocusScheduleDao
import com.minimalphone.core.data.db.entity.FocusPresetEntity
import com.minimalphone.core.data.db.entity.FocusScheduleEntity
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusPreset
import com.minimalphone.core.model.FocusSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultScheduleRepository @Inject constructor(
    private val presetDao: FocusPresetDao,
    private val scheduleDao: FocusScheduleDao
) : ScheduleRepository {

    override fun observePresets(): Flow<List<FocusPreset>> {
        return presetDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getPreset(id: String): FocusPreset? {
        return presetDao.getById(id)?.toDomain()
    }

    override suspend fun savePreset(preset: FocusPreset) {
        presetDao.upsert(
            FocusPresetEntity(
                id = preset.id,
                title = preset.title,
                durationMinutes = preset.durationMinutes,
                mode = preset.mode,
                category = preset.category,
                isDefault = preset.isDefault
            )
        )
    }

    override suspend fun deletePreset(id: String) {
        presetDao.delete(id)
    }

    override suspend fun seedDefaultPresets() {
        val defaults = listOf(
            FocusPresetEntity("preset_deep_work", "Deep Work", 50, FocusMode.DEEP_FOCUS, "WORK", isDefault = true),
            FocusPresetEntity("preset_pomodoro", "Pomodoro Focus", 25, FocusMode.STRICT, "STUDY", isDefault = true),
            FocusPresetEntity("preset_reading", "Digital Detox & Reading", 45, FocusMode.STRICT, "READING", isDefault = true),
            FocusPresetEntity("preset_wind_down", "Night Wind-Down", 60, FocusMode.LIGHT, "REST", isDefault = true)
        )
        presetDao.upsertAll(defaults)
    }

    override fun observeSchedules(): Flow<List<FocusSchedule>> {
        return scheduleDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeActiveSchedules(): Flow<List<FocusSchedule>> {
        return scheduleDao.observeActiveSchedules().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getActiveSchedules(): List<FocusSchedule> {
        return scheduleDao.getActiveSchedules().map { it.toDomain() }
    }

    override suspend fun getSchedule(id: String): FocusSchedule? {
        return scheduleDao.getById(id)?.toDomain()
    }

    override suspend fun saveSchedule(schedule: FocusSchedule) {
        scheduleDao.upsert(
            FocusScheduleEntity(
                id = schedule.id,
                presetId = schedule.presetId,
                title = schedule.title,
                daysOfWeek = schedule.daysOfWeek.joinToString(",") { it.name },
                startTimeHour = schedule.startTime.hour,
                startTimeMinute = schedule.startTime.minute,
                durationMinutes = schedule.durationMinutes,
                mode = schedule.mode,
                isEnabled = schedule.isEnabled
            )
        )
    }

    override suspend fun setScheduleEnabled(id: String, isEnabled: Boolean) {
        scheduleDao.setEnabled(id, isEnabled)
    }

    override suspend fun deleteSchedule(id: String) {
        scheduleDao.delete(id)
    }

    private fun FocusPresetEntity.toDomain() = FocusPreset(
        id = id,
        title = title,
        durationMinutes = durationMinutes,
        mode = mode,
        category = category,
        isDefault = isDefault
    )

    private fun FocusScheduleEntity.toDomain(): FocusSchedule {
        val days = daysOfWeek.split(",")
            .mapNotNull { name -> runCatching { DayOfWeek.valueOf(name.trim()) }.getOrNull() }
            .toSet()

        return FocusSchedule(
            id = id,
            presetId = presetId,
            title = title,
            daysOfWeek = if (days.isEmpty()) DayOfWeek.values().toSet() else days,
            startTime = LocalTime.of(startTimeHour.coerceIn(0, 23), startTimeMinute.coerceIn(0, 59)),
            durationMinutes = durationMinutes,
            mode = mode,
            isEnabled = isEnabled
        )
    }
}
