package com.minimalphone.core.common

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import java.lang.ref.WeakReference

object AccessibilityHelper {

    private var serviceRef: WeakReference<AccessibilityService>? = null

    fun registerService(service: AccessibilityService) {
        serviceRef = WeakReference(service)
    }

    fun unregisterService() {
        serviceRef = null
    }

    fun isServiceConnected(): Boolean {
        return serviceRef?.get() != null
    }

    /**
     * Locks the screen using AccessibilityService on API 28+ (Pie).
     * Zero new permissions and zero window content access.
     */
    fun lockScreen(): Boolean {
        val service = serviceRef?.get() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun isAccessibilityServiceEnabled(
        context: Context,
        serviceClass: Class<out AccessibilityService>
    ): Boolean {
        val expectedComponentName = ComponentName(context, serviceClass)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    fun createAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
