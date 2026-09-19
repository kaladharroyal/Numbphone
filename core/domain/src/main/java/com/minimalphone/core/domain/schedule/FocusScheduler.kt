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
        const val ACTION_FOCUS_SESSION_EXPIRED = "com.minimalphone.ACTION_FOCUS_SESSION_EXPIRED"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_SESSION_ID = "extra_session_id"
        private const val REQUEST_CODE_SCHEDULED_FOCUS = 1001
        private const val REQUEST_CODE_SESSION_EXPIRY = 1002
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

                    val result = startFocusSessionUseCase(
                        goal = FocusGoal(
                            id = schedule.id,
                            title = schedule.title,
                            category = "SCHEDULED",
                            isCustom = false
                        ),
                        durationMinutes = remainingMinutes,
                        mode = schedule.mode
                    )
                    result.getOrNull()?.let { session ->
                        com.minimalphone.core.common.DndHelper.enablePriorityCallsOnlyDnd(context)
                        scheduleSessionExpiryAlarm(session.id, session.endTime)
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * Schedules an AlarmManager wakeup for when the active focus session concludes.
     */
    fun scheduleSessionExpiryAlarm(sessionId: String, endTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(ACTION_FOCUS_SESSION_EXPIRED).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SESSION_EXPIRY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            MinimalLog.i(TAG, "Scheduled session expiry alarm for session '$sessionId' at $endTimeMillis")
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Failed to schedule session expiry alarm", e)
        }
    }

    /**
     * Cancels any pending focus session expiry alarm.
     */
    fun cancelSessionExpiryAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(ACTION_FOCUS_SESSION_EXPIRED).apply {
            setPackage(context.packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SESSION_EXPIRY,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            MinimalLog.i(TAG, "Cancelled session expiry alarm")
        }
    }

    /**
     * Invoked when the session expiry alarm fires in the background.
     */
    suspend fun onSessionExpired(sessionId: String) {
        MinimalLog.i(TAG, "Handling session expiry alarm for session '$sessionId'")
        if (sessionId.isNotBlank()) {
            focusSessionRepository.completeSession(sessionId)
        }
        com.minimalphone.core.common.DndHelper.restoreNormalNotifications(context)
        com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, false)
        scheduleNextAlarm()
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
                REQUEST_CODE_SCHEDULED_FOCUS,
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
