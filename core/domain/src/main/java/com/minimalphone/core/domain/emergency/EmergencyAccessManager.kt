package com.minimalphone.core.domain.emergency

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the emergency / essential app access layer.
 *
 * Essential apps bypass ALL focus restrictions and budget rules,
 * ensuring the user can always make a call, send a message, or access
 * a critical app regardless of active focus sessions or Dumb Mode.
 *
 * Hardcoded system emergency packages always remain in the allow-list.
 * User-configured essential packages (stored in DB) supplement the list.
 */
@Singleton
class EmergencyAccessManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val essentialAppRepository: com.minimalphone.core.data.repository.EssentialAppRepository
) {

    companion object {
        /**
         * System-level emergency packages that are ALWAYS allowed regardless of
         * user configuration. These cannot be removed by the user.
         */
        val SYSTEM_EMERGENCY_PACKAGES: Set<String> = setOf(
            "com.android.emergency",           // Android Emergency
            "com.android.phone",               // AOSP phone
            "com.android.dialer",              // AOSP dialer
            "com.samsung.android.dialer",      // Samsung dialer
            "com.google.android.dialer",       // Google dialer
            "com.oneplus.dialer",              // OnePlus dialer
            "com.motorola.phone",              // Motorola phone
            "com.sec.android.emergencylauncher" // Samsung SOS
        )

        /**
         * Intent-based essential categories resolved at runtime.
         * These are the 6 default essential app types.
         */
        val ESSENTIAL_INTENTS: List<Intent> = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_MESSAGING) },
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CONTACTS) },
            Intent("android.intent.action.SET_ALARM"),
            Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALENDAR) }
        )
    }

    /**
     * Returns true if the given package should bypass all restrictions.
     * Priority: system emergency → user-configured essential apps.
     */
    suspend fun isEmergencyOrEssential(packageName: String): Boolean {
        if (packageName in SYSTEM_EMERGENCY_PACKAGES) return true
        val userEssentials = essentialAppRepository.getEssentialPackages()
        return packageName in userEssentials
    }

    /**
     * Observes the user-configured essential packages as a Flow.
     */
    fun observeEssentialPackages(): Flow<Set<String>> =
        essentialAppRepository.observeEssentialPackages()

    /**
     * Resolves which packages handle the 6 essential app categories on this device.
     * Used during bootstrapping and on the Essential Access settings screen.
     */
    fun resolveDefaultEssentialPackages(): Set<String> {
        val pm = context.packageManager
        val resolved = mutableSetOf<String>()
        for (intent in ESSENTIAL_INTENTS) {
            try {
                val info = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveActivity(intent, 0)
                }
                info?.activityInfo?.packageName?.let { resolved.add(it) }
            } catch (_: Exception) {}
        }
        // Always include system emergency packages that are installed
        for (pkg in SYSTEM_EMERGENCY_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                resolved.add(pkg)
            } catch (_: PackageManager.NameNotFoundException) {}
        }
        return resolved
    }

    /**
     * Attempts to launch the system dialer directly — used by the SOS button.
     */
    fun getDialerIntent(): Intent =
        Intent(Intent.ACTION_DIAL).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
