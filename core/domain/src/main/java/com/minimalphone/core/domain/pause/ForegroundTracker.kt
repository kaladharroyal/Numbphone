package com.minimalphone.core.domain.pause

import javax.inject.Inject
import javax.inject.Singleton

data class ForegroundTransition(
    val previousPackage: String?,
    val currentPackage: String
)

@Singleton
class ForegroundTracker @Inject constructor() {

    private var currentForegroundPackage: String? = null
    private var lastEventTimeMs: Long = 0L

    companion object {
        const val DEBOUNCE_MS = 300L
        val DEFAULT_IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "com.google.android.googlequicksearchbox"
        )
    }

    fun getCurrentForegroundPackage(): String? = currentForegroundPackage

    fun reset() {
        currentForegroundPackage = null
        lastEventTimeMs = 0L
    }

    /**
     * Determines whether an incoming window change event should be ignored.
     */
    fun shouldIgnore(
        packageName: String,
        className: String? = null,
        enabledImePackages: Set<String> = emptySet(),
        ownPackageName: String = ""
    ): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == ownPackageName) return true
        if (DEFAULT_IGNORED_PACKAGES.contains(packageName)) return true
        if (enabledImePackages.contains(packageName)) return true
        if (packageName.contains("inputmethod", ignoreCase = true)) return true
        return false
    }

    /**
     * Evaluates a foreground window change with debouncing and filter checks.
     * Returns a [ForegroundTransition] if a genuine app transition took place, or null otherwise.
     */
    fun onWindowStateChanged(
        packageName: String,
        className: String? = null,
        enabledImePackages: Set<String> = emptySet(),
        ownPackageName: String = "",
        nowMs: Long = System.currentTimeMillis()
    ): ForegroundTransition? {
        if (shouldIgnore(packageName, className, enabledImePackages, ownPackageName)) {
            return null
        }

        // Debounce rapid window changes for the exact same package
        if (packageName == currentForegroundPackage && (nowMs - lastEventTimeMs) < DEBOUNCE_MS) {
            return null
        }

        lastEventTimeMs = nowMs

        if (packageName != currentForegroundPackage) {
            val transition = ForegroundTransition(
                previousPackage = currentForegroundPackage,
                currentPackage = packageName
            )
            currentForegroundPackage = packageName
            return transition
        }

        return null
    }
}
