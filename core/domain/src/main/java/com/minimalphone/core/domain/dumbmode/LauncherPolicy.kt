package com.minimalphone.core.domain.dumbmode

import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.InstalledApp
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines which apps are visible on the Home Screen based on user favorites,
 * current focus session state, and Dumb Mode policy.
 */
@Singleton
class LauncherPolicy @Inject constructor() {

    /**
     * Filters apps for the Home Screen:
     * - If [isDumbMode] is true, only essential apps (or apps labeled Phone/Messages/Emergency) are visible.
     * - If [isDumbMode] is false, all favorite/whitelisted home apps are returned.
     */
    fun filterVisibleApps(
        apps: List<InstalledApp>,
        isDumbMode: Boolean,
        essentialPackages: Set<String> = emptySet()
    ): List<InstalledApp> {
        if (!isDumbMode) {
            return apps
        }

        return apps.filter { app ->
            app.isEssential ||
                    app.category == AppCategory.ESSENTIAL ||
                    essentialPackages.contains(app.packageName) ||
                    app.label.equals("Phone", ignoreCase = true) ||
                    app.label.equals("Messages", ignoreCase = true) ||
                    app.label.equals("Dialer", ignoreCase = true) ||
                    app.label.equals("Settings", ignoreCase = true)
        }
    }
}
