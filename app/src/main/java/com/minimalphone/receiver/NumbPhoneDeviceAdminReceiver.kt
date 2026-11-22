package com.minimalphone.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.minimalphone.core.common.MinimalLog

class NumbPhoneDeviceAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "DeviceAdminReceiver"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        MinimalLog.i(TAG, "Device admin enabled — uninstall lock is active")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Disabling Device Admin removes NumbPhone's uninstall protection. " +
               "You will need to complete the focus session exit procedure first."
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        MinimalLog.i(TAG, "Device admin disabled — uninstall lock deactivated")
    }
}
