package com.minimalphone.core.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object GrayscaleHelper {

    private const val ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val ACCESSIBILITY_DISPLAY_DALTONIZER = "accessibility_display_daltonizer"
    private const val DALTONIZER_DISABLED = -1
    private const val DALTONIZER_MONOCHROMACY = 0

    const val ADB_PERMISSION_COMMAND = "adb shell pm grant com.minimalphone android.permission.WRITE_SECURE_SETTINGS"

    /**
     * Checks whether the app holds WRITE_SECURE_SETTINGS permission.
     */
    fun hasSecureSettingsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks whether system-level grayscale is currently enabled.
     */
    fun isGrayscaleEnabled(context: Context): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                context.contentResolver,
                ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                0
            ) == 1
            val mode = Settings.Secure.getInt(
                context.contentResolver,
                ACCESSIBILITY_DISPLAY_DALTONIZER,
                DALTONIZER_DISABLED
            )
            enabled && mode == DALTONIZER_MONOCHROMACY
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Toggles system-level grayscale on or off.
     * Requires WRITE_SECURE_SETTINGS permission granted via ADB.
     *
     * @return true if successfully changed, false if permission missing or failed.
     */
    fun setGrayscaleEnabled(context: Context, enable: Boolean): Boolean {
        if (!hasSecureSettingsPermission(context)) {
            return false
        }
        return try {
            if (enable) {
                Settings.Secure.putInt(
                    context.contentResolver,
                    ACCESSIBILITY_DISPLAY_DALTONIZER,
                    DALTONIZER_MONOCHROMACY
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                    1
                )
            } else {
                Settings.Secure.putInt(
                    context.contentResolver,
                    ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                    0
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    ACCESSIBILITY_DISPLAY_DALTONIZER,
                    DALTONIZER_DISABLED
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens Android's Color Correction / Accessibility display settings
     * so user can manually toggle Grayscale if WRITE_SECURE_SETTINGS is not granted.
     */
    fun openColorCorrectionSettings(context: Context) {
        val intents = listOf(
            Intent("android.settings.COLOR_CORRECTION_SETTINGS").apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
            Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )

        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }
}
