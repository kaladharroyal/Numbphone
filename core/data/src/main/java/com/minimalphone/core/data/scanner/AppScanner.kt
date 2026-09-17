package com.minimalphone.core.data.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.UserManager
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.ClassificationSource
import com.minimalphone.core.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager
    private val launcherApps: LauncherApps? = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    private val userManager: UserManager? = context.getSystemService(Context.USER_SERVICE) as? UserManager

    fun scanAllLaunchableApps(): List<InstalledApp> {
        val result = mutableListOf<InstalledApp>()
        val seenPackages = mutableSetOf<String>()

        try {
            if (launcherApps != null && userManager != null) {
                val profiles = userManager.userProfiles
                for (profile in profiles) {
                    val activityList: List<LauncherActivityInfo> = launcherApps.getActivityList(null, profile)
                    for (activityInfo in activityList) {
                        val pkgName = activityInfo.applicationInfo.packageName
                        if (pkgName == context.packageName) {
                            // Don't include ourselves in the list of launched apps
                            continue
                        }
                        seenPackages.add(pkgName)

                        val label = activityInfo.label.toString()
                        val isSystem = (activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isEssential = isEssentialPackage(pkgName, label)

                        result.add(
                            InstalledApp(
                                packageName = pkgName,
                                activityName = activityInfo.componentName.className,
                                label = label,
                                category = if (isEssential) AppCategory.ESSENTIAL else AppCategory.MANAGED,
                                isSystemApp = isSystem,
                                isEssential = isEssential,
                                isBlockedInFocus = !isEssential,
                                isFavoriteOnHome = isEssential,
                                lastUsedTimestamp = 0L,
                                installedTimestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Error scanning apps via LauncherApps API", e)
        }

        // Fallback or supplementary check with PackageManager
        if (result.isEmpty()) {
            try {
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        mainIntent,
                        PackageManager.ResolveInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(mainIntent, 0)
                }

                for (resolveInfo in resolveInfos) {
                    val pkgName = resolveInfo.activityInfo.packageName
                    if (pkgName == context.packageName || seenPackages.contains(pkgName)) {
                        continue
                    }
                    val label = resolveInfo.loadLabel(packageManager).toString()
                    val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isEssential = isEssentialPackage(pkgName, label)

                    result.add(
                        InstalledApp(
                            packageName = pkgName,
                            activityName = resolveInfo.activityInfo.name,
                            label = label,
                            category = if (isEssential) AppCategory.ESSENTIAL else AppCategory.MANAGED,
                            isSystemApp = isSystem,
                            isEssential = isEssential,
                            isBlockedInFocus = !isEssential,
                            isFavoriteOnHome = isEssential,
                            lastUsedTimestamp = 0L,
                            installedTimestamp = System.currentTimeMillis()
                        )
                    )
                }
            } catch (e: Exception) {
                MinimalLog.e(TAG, "Error scanning apps via PackageManager fallback", e)
            }
        }

        MinimalLog.i(TAG, "Discovered ${result.size} launchable applications on device")
        return result.sortedBy { it.label.lowercase() }
    }

    fun getAppDetails(packageName: String): InstalledApp? {
        return try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            val activityName = launchIntent?.component?.className ?: ""
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isEssential = isEssentialPackage(packageName, label)

            InstalledApp(
                packageName = packageName,
                activityName = activityName,
                label = label,
                category = if (isEssential) AppCategory.ESSENTIAL else AppCategory.MANAGED,
                isSystemApp = isSystem,
                isEssential = isEssential,
                isBlockedInFocus = !isEssential,
                isFavoriteOnHome = isEssential
            )
        } catch (e: Exception) {
            MinimalLog.w(TAG, "App details not found for $packageName", e)
            null
        }
    }

    companion object {
        private const val TAG = "AppScanner"

        /**
         * The 6 curated essential apps shown on the Minimal Home Screen by default.
         * Detected by label OR package name to be OEM-agnostic.
         *   1. Phone / Dialer
         *   2. Messages / SMS
         *   3. Camera
         *   4. Clock / Alarm
         *   5. Contacts
         *   6. Calendar
         */
        fun isEssentialPackage(packageName: String, label: String): Boolean {
            val lowerPkg = packageName.lowercase()
            val lowerLabel = label.lowercase()

            // 1. Phone / Dialer
            if (lowerLabel == "phone" || lowerLabel == "dialer" ||
                lowerPkg.contains("dialer") || lowerPkg.contains("incallui") ||
                lowerPkg == "com.android.phone"
            ) return true

            // 2. Messages / SMS
            if (lowerLabel == "messages" || lowerLabel == "sms" || lowerLabel == "messenger" ||
                lowerPkg.contains("messaging") || lowerPkg.contains(".mms") ||
                lowerPkg == "com.google.android.apps.messaging"
            ) return true

            // 3. Camera
            if (lowerLabel == "camera" ||
                lowerPkg.contains("camera") && !lowerPkg.contains("camerax")
            ) return true

            // 4. Clock / Alarm
            if (lowerLabel == "clock" || lowerLabel == "alarm" ||
                lowerPkg.contains("deskclock") || lowerPkg.contains("clock")
            ) return true

            // 5. Contacts
            if (lowerLabel == "contacts" || lowerLabel == "people" ||
                lowerPkg.contains("contacts") && !lowerPkg.contains("contactscommon")
            ) return true

            // 6. Calendar
            if (lowerLabel == "calendar" ||
                lowerPkg.contains("calendar")
            ) return true

            // Explicitly NOT essential — distracting apps
            if (lowerPkg.contains("youtube") || lowerPkg.contains("instagram") ||
                lowerPkg.contains("tiktok") || lowerPkg.contains("twitter") ||
                lowerPkg.contains("reddit") || lowerPkg.contains("netflix") ||
                lowerPkg.contains("facebook") || lowerPkg.contains("snapchat") ||
                lowerPkg.contains("game")
            ) return false

            return false
        }
    }
}
