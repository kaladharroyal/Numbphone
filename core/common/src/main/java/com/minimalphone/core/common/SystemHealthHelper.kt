package com.minimalphone.core.common

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object SystemHealthHelper {

    fun isDefaultLauncher(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
                }
            }

            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = context.packageManager.resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName == context.packageName
        } catch (_: Exception) {
            false
        }
    }

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun openDefaultLauncherSettings(context: Context) {
        val intents = listOf(
            Intent(Settings.ACTION_HOME_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = context.getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                MinimalLog.w("SystemHealthHelper", "RoleManager fallback failed", e)
            }
        }
    }

    fun createDefaultLauncherSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_HOME_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createBatteryOptimizationSettingsIntent(context: Context): Intent {
        return try {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } catch (_: Exception) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    fun getOemGuidance(): String? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("samsung") ->
                "Samsung OneUI: In Device Care > Battery > Background usage limits, add NumbPhone to 'Never sleeping apps'."
            isMiui() ->
                "MIUI: Go to Apps > Manage Apps > NumbPhone > Battery saver > select 'No restrictions'."
            isEmui() ->
                "EMUI: Go to Settings > Battery > App Launch > NumbPhone > set to 'Manage manually'."
            isColorOs() ->
                "ColorOS/Realme: In App info > Battery usage, enable 'Allow background activity'."
            isOxygenOs() ->
                "OxygenOS: Set Battery optimization to 'Don't optimize' and lock NumbPhone in Recent Apps."
            isHuawei() ->
                "Huawei/Honor: In Battery > App Launch, set NumbPhone to 'Manage manually' with all toggles ON."
            else -> null
        }
    }

    private fun isMiui(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco")
    }

    private fun isEmui(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("huawei")
    }

    private fun isColorOs(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("oppo") || m.contains("realme")
    }

    private fun isOxygenOs(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("oneplus")
    }

    private fun isHuawei(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        return m.contains("huawei") || m.contains("honor")
    }
}
