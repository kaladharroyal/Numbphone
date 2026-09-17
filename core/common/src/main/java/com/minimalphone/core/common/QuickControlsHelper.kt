package com.minimalphone.core.common

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings

enum class AudioMode(val label: String) {
    NORMAL("Sound"),
    VIBRATE("Vibrate"),
    SILENT("Silent")
}

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean
)

object QuickControlsHelper {
    private const val TAG = "QuickControlsHelper"
    private var isTorchActive = false

    fun isTorchOn(): Boolean = isTorchActive

    fun toggleTorch(context: Context): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return false
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return false

            isTorchActive = !isTorchActive
            cameraManager.setTorchMode(cameraId, isTorchActive)
            isTorchActive
        } catch (e: Exception) {
            MinimalLog.w(TAG, "Failed to toggle torch", e)
            false
        }
    }

    fun getAudioMode(context: Context): AudioMode {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return AudioMode.NORMAL
            when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> AudioMode.SILENT
                AudioManager.RINGER_MODE_VIBRATE -> AudioMode.VIBRATE
                else -> AudioMode.NORMAL
            }
        } catch (_: Exception) {
            AudioMode.NORMAL
        }
    }

    fun cycleAudioMode(context: Context): AudioMode {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return AudioMode.NORMAL
            val nextMode = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            audioManager.ringerMode = nextMode
            getAudioMode(context)
        } catch (e: Exception) {
            MinimalLog.w(TAG, "Failed to change audio mode", e)
            getAudioMode(context)
        }
    }

    fun getBatteryInfo(context: Context): BatteryInfo {
        return try {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            BatteryInfo(percentage = pct, isCharging = isCharging)
        } catch (_: Exception) {
            BatteryInfo(percentage = 100, isCharging = false)
        }
    }

    fun openWifiSettings(context: Context) {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemSettings(context)
        }
    }

    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            openSystemSettings(context)
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            DndHelper.openNotificationPolicySettings(context)
        }
    }

    fun openSystemSettings(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
