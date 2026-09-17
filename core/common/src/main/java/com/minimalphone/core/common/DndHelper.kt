package com.minimalphone.core.common

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object DndHelper {
    private const val TAG = "DndHelper"

    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted ?: false
    }

    fun openNotificationPolicySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Failed to open notification policy settings", e)
        }
    }

    fun enablePriorityCallsOnlyDnd(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                MinimalLog.i(TAG, "DND Priority mode (Calls Only) enabled.")
                true
            } else {
                MinimalLog.w(TAG, "Notification policy access not granted; unable to set DND")
                false
            }
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Failed to set DND interruption filter", e)
            false
        }
    }

    fun restoreNormalNotifications(context: Context): Boolean {
        return try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                MinimalLog.i(TAG, "Restored normal notification interruption filter.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Failed to restore notification filter", e)
            false
        }
    }
}
