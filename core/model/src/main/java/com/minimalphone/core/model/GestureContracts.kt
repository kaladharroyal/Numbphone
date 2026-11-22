package com.minimalphone.core.model

/**
 * Physical gestures supported on the Home Screen.
 */
enum class GestureType {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    DOUBLE_TAP,
    LONG_PRESS_CLOCK
}

/**
 * Configurable actions that can be bound to home gestures.
 */
sealed interface GestureAction {
    data object None : GestureAction
    data object OpenSearch : GestureAction
    data object OpenQuickControl : GestureAction
    data object OpenSettings : GestureAction
    data object OpenFocusScreen : GestureAction
    data class LaunchApp(val packageName: String) : GestureAction
    data object LockScreen : GestureAction

    companion object {
        fun serialize(action: GestureAction): String = when (action) {
            is None -> "NONE"
            is OpenSearch -> "OPEN_SEARCH"
            is OpenQuickControl -> "OPEN_QUICK_CONTROL"
            is OpenSettings -> "OPEN_SETTINGS"
            is OpenFocusScreen -> "OPEN_FOCUS_SCREEN"
            is LaunchApp -> "LAUNCH_APP:${action.packageName}"
            is LockScreen -> "LOCK_SCREEN"
        }

        fun deserialize(value: String): GestureAction {
            return when {
                value == "NONE" -> None
                value == "OPEN_SEARCH" -> OpenSearch
                value == "OPEN_QUICK_CONTROL" -> OpenQuickControl
                value == "OPEN_SETTINGS" -> OpenSettings
                value == "OPEN_FOCUS_SCREEN" -> OpenFocusScreen
                value.startsWith("LAUNCH_APP:") -> {
                    val pkg = value.substringAfter("LAUNCH_APP:")
                    if (pkg.isNotBlank()) LaunchApp(pkg) else None
                }
                value == "LOCK_SCREEN" -> LockScreen
                else -> None
            }
        }

        fun serializeMap(config: Map<GestureType, GestureAction>): String {
            return config.entries.joinToString(";") { "${it.key.name}=${serialize(it.value)}" }
        }

        fun deserializeMap(serialized: String): Map<GestureType, GestureAction> {
            if (serialized.isBlank()) return defaultGestureConfig()
            val result = defaultGestureConfig().toMutableMap()
            serialized.split(";").forEach { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    val type = runCatching { GestureType.valueOf(parts[0]) }.getOrNull()
                    if (type != null) {
                        result[type] = deserialize(parts[1])
                    }
                }
            }
            return result
        }

        /**
         * System defaults as specified in M23.4:
         * Swipe up: Open search
         * Swipe down: Open Quick Control Center
         * Swipe left: Launch Camera
         * Swipe right: Launch Phone
         * Double tap: Lock screen
         * Long-press clock: Open Settings
         */
        fun defaultGestureConfig(): Map<GestureType, GestureAction> = mapOf(
            GestureType.SWIPE_UP to OpenSearch,
            GestureType.SWIPE_DOWN to OpenQuickControl,
            GestureType.SWIPE_LEFT to LaunchApp("com.android.camera"),
            GestureType.SWIPE_RIGHT to LaunchApp("com.google.android.dialer"),
            GestureType.DOUBLE_TAP to LockScreen,
            GestureType.LONG_PRESS_CLOCK to OpenSettings
        )
    }
}
