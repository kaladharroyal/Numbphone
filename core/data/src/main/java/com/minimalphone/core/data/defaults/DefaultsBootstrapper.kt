package com.minimalphone.core.data.defaults

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.dao.InstalledAppDao
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.ClassificationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bootstraps the 6 essential default apps on first install.
 * Uses Android Intents to dynamically resolve the actual app package on any OEM device.
 *
 * Essentials:
 *   1. Phone / Dialer
 *   2. Messages / SMS
 *   3. Camera
 *   4. Clock / Alarm
 *   5. Contacts
 *   6. Calendar
 */
@Singleton
class DefaultsBootstrapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppDao: InstalledAppDao,
    private val appRuleDao: AppRuleDao
) {

    companion object {
        private const val TAG = "DefaultsBootstrapper"
        private const val PREFS_NAME = "minimal_defaults"
        private const val KEY_BOOTSTRAPPED = "defaults_bootstrapped_v1"
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isBootstrapped: Boolean
        get() = prefs.getBoolean(KEY_BOOTSTRAPPED, false)

    /**
     * Resolves the 6 essential default apps using Android system Intents.
     * Returns the set of resolved package names.
     */
    private fun resolveEssentialPackages(): Set<String> {
        val pm = context.packageManager
        val packages = mutableSetOf<String>()

        // 1. Phone / Dialer
        resolvePackage(pm, Intent(Intent.ACTION_DIAL))?.let { packages.add(it) }

        // 2. Messages / SMS
        resolvePackage(pm, Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "vnd.android-dir/mms-sms"
        })?.let { packages.add(it) }
            ?: resolvePackage(pm, Intent("android.intent.action.MAIN").apply {
                addCategory("android.intent.category.APP_MESSAGING")
            })?.let { packages.add(it) }

        // 3. Camera
        resolvePackage(pm, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            ?.let { packages.add(it) }
            ?: resolvePackage(pm, Intent(MediaStore.ACTION_IMAGE_CAPTURE))
                ?.let { packages.add(it) }

        // 4. Clock / Alarm
        resolvePackage(pm, Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS))
            ?.let { packages.add(it) }
            ?: resolvePackage(pm, Intent(android.provider.AlarmClock.ACTION_SET_ALARM))
                ?.let { packages.add(it) }

        // 5. Contacts
        resolvePackage(pm, Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CONTACTS)
        })?.let { packages.add(it) }
            ?: resolvePackage(pm, Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI))
                ?.let { packages.add(it) }

        // 6. Calendar
        resolvePackage(pm, Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CALENDAR)
        })?.let { packages.add(it) }
            ?: resolvePackage(pm, Intent(Intent.ACTION_VIEW, CalendarContract.CONTENT_URI))
                ?.let { packages.add(it) }

        MinimalLog.i(TAG, "Resolved essential packages: $packages")
        return packages
    }

    private fun resolvePackage(pm: PackageManager, intent: Intent): String? {
        return try {
            val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.resolveActivity(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.resolveActivity(intent, 0)
            }
            resolveInfo?.activityInfo?.packageName?.takeIf {
                it != context.packageName && it.isNotBlank()
            }
        } catch (e: Exception) {
            MinimalLog.w(TAG, "Could not resolve intent: ${intent.action}", e)
            null
        }
    }

    /**
     * Run on first launch only. Marks discovered essential apps as isFavoriteOnHome = true
     * and persists an AppRule so user overrides are respected on subsequent syncs.
     */
    suspend fun bootstrapIfNeeded() = withContext(Dispatchers.IO) {
        if (isBootstrapped) return@withContext

        val essentialPackages = resolveEssentialPackages()
        if (essentialPackages.isEmpty()) {
            MinimalLog.w(TAG, "No essential packages resolved — skipping bootstrap")
            return@withContext
        }

        var pinned = 0
        for (pkg in essentialPackages) {
            try {
                val app = installedAppDao.getApp(pkg) ?: continue

                // Only update if not already user-overridden
                if (app.classificationSource != ClassificationSource.USER_OVERRIDE) {
                    // Pin to home
                    installedAppDao.updateFavorite(pkg, true)
                    // Update category to ESSENTIAL
                    installedAppDao.updateCategory(
                        packageName = pkg,
                        category = AppCategory.ESSENTIAL,
                        isEssential = true,
                        isBlocked = false
                    )
                    // Persist rule so future syncs preserve user's home screen
                    appRuleDao.upsertRule(
                        AppRuleEntity(
                            packageName = pkg,
                            category = AppCategory.ESSENTIAL,
                            isAlwaysAvailable = true,
                            isBlockedInFocus = false,
                            isFavoriteOnHome = true,
                            classificationSource = ClassificationSource.SYSTEM_PRESET
                        )
                    )
                    pinned++
                    MinimalLog.i(TAG, "Pinned essential to home: ${app.label} ($pkg)")
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error pinning essential app $pkg", e)
            }
        }

        // Mark as bootstrapped so this only runs once
        prefs.edit().putBoolean(KEY_BOOTSTRAPPED, true).apply()
        MinimalLog.i(TAG, "Bootstrapped $pinned/${essentialPackages.size} essential apps to home screen")
    }
}
