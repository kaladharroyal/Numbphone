package com.minimalphone.core.common

import android.content.ComponentName
import android.content.Context

object DeviceAdminHelper {
    const val RECEIVER_CLASS_NAME = "com.minimalphone.receiver.NumbPhoneDeviceAdminReceiver"

    fun getAdminComponentName(context: Context): ComponentName =
        ComponentName(context.packageName, RECEIVER_CLASS_NAME)
}
