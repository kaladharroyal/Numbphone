package com.minimalphone.core.domain.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.ScheduleRepository
import com.minimalphone.core.domain.focus.StartFocusSessionUseCase
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusSchedule
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val startFocusSessionUseCase: StartFocusSessionUseCase
) {
    companion object {
        private const val TAG = "FocusScheduler"
        const val ACTION_TRIGGER_SCHEDULED_FOCUS = "com.minimalphone.ACTION_TRIGGER_SCHEDULED_FOCUS"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    }

    /**
     * Checks all enabled schedules against the current day/time.
     * If a schedule is active and no focus session is currently running, starts one automatically.
     */
    suspend fun evaluateScheduledFocus(): Boolean {
        val activeSession = focusSessionRepository.getActiveSessionSync()
        if (activeSession != null && activeSession.isCurrentlyActive) {
            return false
        }

        val now = LocalDateTime.now()
        val currentDay = now.dayOfWeek
        val currentTime = now.toLocalTime()

        val activeSchedules = scheduleRepository.getActiveSchedules()
        for (schedule in activeSchedules) {
            if (schedule.daysOfWeek.contains(currentDay)) {
                val start = schedule.startTime
                val end = start.plusMinutes(schedule.durationMinutes.toLong())

                val isInWindow = if (end.isAfter(start)) {
                    !currentTime.isBefore(start) && currentTime.isBefore(end)
                } else {
                    // Spans midnight
                    !currentTime.isBefore(start) || currentTime.isBefore(end)
                }

                if (isInWindow) {
                    val remainingMinutes = java.time.Duration.between(currentTime, end).toMinutes().toInt().coerceAtLeast(1)
                    MinimalLog.i(TAG, "Starting scheduled focus '${schedule.title}' for remaining ${remainingMinutes}m")

                    startFocusSessionUseCase(
                        goal = FocusGoal(
                            id = schedule.id,
                            title = schedule.title,
                            category = "SCHEDULED",
                            isCustom = false
                        ),
                        durationMinutes = remainingMinutes,
                        mode = schedule.mode
                    )
                    return true
                }
            }
        }
        return false
    }

    /**
     * Reschedules the next AlarmManager wakeup for upcoming scheduled focus sessions.
     */
    suspend fun scheduleNextAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val schedules = scheduleRepository.getActiveSchedules()
        if (schedules.isEmpty()) return

        val now = LocalDateTime.now()
        var nextTrigger: LocalDateTime? = null
        var nextSchedule: FocusSchedule? = null

        for (schedule in schedules) {
            for (dayOffset in 0..7) {
                val targetDate = now.toLocalDate().plusDays(dayOffset.toLong())
                val dayOfWeek = targetDate.dayOfWeek
                if (schedule.daysOfWeek.contains(dayOfWeek)) {
                    val triggerDateTime = LocalDateTime.of(targetDate, schedule.startTime)
                    if (triggerDateTime.isAfter(now)) {
                        if (nextTrigger == null || triggerDateTime.isBefore(nextTrigger)) {
                            nextTrigger = triggerDateTime
                            nextSchedule = schedule
                        }
                    }
                }
            }
        }

        if (nextTrigger != null && nextSchedule != null) {
            val epochMillis = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = Intent(ACTION_TRIGGER_SCHEDULED_FOCUS).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_SCHEDULE_ID, nextSchedule.id)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
                MinimalLog.i(TAG, "Scheduled next focus alarm for '${nextSchedule.title}' at $nextTrigger")
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Failed to schedule exact alarm", e)
            }
        }
    }
}
