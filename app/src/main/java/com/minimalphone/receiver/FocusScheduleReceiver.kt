package com.minimalphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.domain.schedule.FocusScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FocusScheduleReceiver : BroadcastReceiver() {

    @Inject
    lateinit var focusScheduler: FocusScheduler

    companion object {
        private const val TAG = "FocusScheduleReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != FocusScheduler.ACTION_TRIGGER_SCHEDULED_FOCUS &&
            action != FocusScheduler.ACTION_FOCUS_SESSION_EXPIRED &&
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        MinimalLog.i(TAG, "Triggering scheduled focus evaluation on action $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (action == FocusScheduler.ACTION_FOCUS_SESSION_EXPIRED) {
                    val sessionId = intent.getStringExtra(FocusScheduler.EXTRA_SESSION_ID) ?: ""
                    focusScheduler.onSessionExpired(sessionId)
                } else {
                    focusScheduler.evaluateScheduledFocus()
                    focusScheduler.scheduleNextAlarm()
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error executing scheduled focus receiver action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
