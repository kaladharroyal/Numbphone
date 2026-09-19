package com.minimalphone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.domain.boot.BootRecoveryResult
import com.minimalphone.core.domain.boot.RestoreFocusOnBootUseCase
import com.minimalphone.core.domain.schedule.FocusScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MinimalBootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var restoreFocusOnBootUseCase: RestoreFocusOnBootUseCase

    @Inject
    lateinit var focusScheduler: FocusScheduler

    companion object {
        private const val TAG = "MinimalBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            return
        }

        MinimalLog.i(TAG, "Device boot / package update detected ($action). Initiating state restoration.")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (val result = restoreFocusOnBootUseCase()) {
                    is BootRecoveryResult.Restored -> {
                        val remainingMinutes = result.session.remainingMillis / 60000L
                        focusScheduler.scheduleSessionExpiryAlarm(result.session.id, result.session.endTime)
                        MinimalLog.i(
                            TAG,
                            "Restored active focus session '${result.session.id}' (${result.session.goal.title}) remaining: ${remainingMinutes}m."
                        )
                    }
                    is BootRecoveryResult.ExpiredAndCompleted -> {
                        com.minimalphone.core.common.DndHelper.restoreNormalNotifications(context)
                        com.minimalphone.core.common.GrayscaleHelper.setGrayscaleEnabled(context, false)
                        MinimalLog.i(
                            TAG,
                            "Session '${result.session.id}' expired while offline. Completed automatically."
                        )
                    }
                    is BootRecoveryResult.NoActiveSession -> {
                        MinimalLog.i(TAG, "No active focus session required restoration on boot.")
                    }
                }

                focusScheduler.evaluateScheduledFocus()
                focusScheduler.scheduleNextAlarm()
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error restoring state during boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
