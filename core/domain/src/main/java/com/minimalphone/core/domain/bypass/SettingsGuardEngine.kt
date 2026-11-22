package com.minimalphone.core.domain.bypass

import com.minimalphone.core.model.SettingsGuardDecision
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class SettingsGuardConfig(
    val blockedClasses: List<String>,
    val allowedClasses: List<String>,
    val settingsPackages: List<String>
)

@Singleton
class SettingsGuardEngine @Inject constructor() {

    private val overrideWindowExpiresAt = AtomicLong(0L)

    companion object {
        const val OVERRIDE_WINDOW_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    }

    /**
     * Activates a 15-minute emergency override window after friction is completed.
     */
    fun grantEmergencyOverride(now: Long = System.currentTimeMillis()) {
        overrideWindowExpiresAt.set(now + OVERRIDE_WINDOW_DURATION_MS)
    }

    /**
     * Returns whether the emergency override window is currently active.
     */
    fun isEmergencyOverrideActive(now: Long = System.currentTimeMillis()): Boolean {
        return now < overrideWindowExpiresAt.get()
    }

    /**
     * Clear override window.
     */
    fun clearEmergencyOverride() {
        overrideWindowExpiresAt.set(0L)
    }

    fun evaluate(
        packageName: String,
        className: String,
        config: SettingsGuardConfig,
        isNumbPhoneTarget: Boolean = false,
        now: Long = System.currentTimeMillis()
    ): SettingsGuardDecision {
        // If emergency override is active, allow all settings access
        if (isEmergencyOverrideActive(now)) {
            return SettingsGuardDecision.Allow
        }

        // 1. Uninstaller classes -> block
        if (isUninstallerClass(className)) {
            return SettingsGuardDecision.Block("UNINSTALL_GUARD")
        }

        // 2. Package installer targeting NumbPhone -> block
        if (isNumbPhoneTarget && isInstallerPackage(packageName)) {
            return SettingsGuardDecision.Block("UNINSTALL_GUARD")
        }

        // 3. Not a settings package -> allow
        if (!isSettingsPackage(packageName, config)) {
            return SettingsGuardDecision.Allow
        }

        // 4. Check blocked classes
        val classSimple = className.substringAfterLast('.')
        if (config.blockedClasses.any { bc ->
                classSimple.equals(bc, ignoreCase = true) ||
                className.contains(bc, ignoreCase = true) ||
                bc.contains(classSimple, ignoreCase = true)
            }) {
            return SettingsGuardDecision.Block("SETTINGS_GUARD")
        }

        // 5. Check allowed classes
        if (config.allowedClasses.any { ac ->
                classSimple.equals(ac, ignoreCase = true) ||
                className.contains(ac, ignoreCase = true) ||
                ac.contains(classSimple, ignoreCase = true)
            }) {
            return SettingsGuardDecision.Allow
        }

        // 6. Unknown settings class -> allow and log
        return SettingsGuardDecision.AllowLogged(className)
    }

    private fun isSettingsPackage(packageName: String, config: SettingsGuardConfig): Boolean {
        return config.settingsPackages.any { it.equals(packageName, ignoreCase = true) } ||
               packageName.endsWith(".settings", ignoreCase = true)
    }

    private fun isInstallerPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains("packageinstaller")
    }

    private fun isUninstallerClass(className: String): Boolean {
        val lower = className.lowercase()
        return lower.contains("uninstaller") || lower.contains("uninstallactivity")
    }
}
