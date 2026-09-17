package com.minimalphone.core.data.repository

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import com.minimalphone.core.common.MinimalLog
import com.minimalphone.core.data.db.dao.AppRuleDao
import com.minimalphone.core.data.db.dao.InstalledAppDao
import com.minimalphone.core.data.db.entity.AppRuleEntity
import com.minimalphone.core.data.db.entity.InstalledAppEntity
import com.minimalphone.core.data.scanner.AppScanner
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.ClassificationSource
import com.minimalphone.core.model.InstalledApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appScanner: AppScanner,
    private val installedAppDao: InstalledAppDao,
    private val appRuleDao: AppRuleDao
) : AppRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps: LauncherApps? = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps.Callback: onPackageAdded ($packageName)")
            scope.launch { onPackageAdded(packageName) }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps.Callback: onPackageRemoved ($packageName)")
            scope.launch { onPackageRemoved(packageName) }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps.Callback: onPackageChanged ($packageName)")
            scope.launch { onPackageChanged(packageName) }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            MinimalLog.i(TAG, "LauncherApps.Callback: onPackagesAvailable (${packageNames.joinToString()})")
            scope.launch { syncInstalledApps() }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            MinimalLog.i(TAG, "LauncherApps.Callback: onPackagesUnavailable (${packageNames.joinToString()})")
            scope.launch { syncInstalledApps() }
        }
    }

    init {
        registerLauncherCallback()
        scope.launch {
            syncInstalledApps()
        }
    }

    private fun registerLauncherCallback() {
        try {
            launcherApps?.registerCallback(launcherCallback, Handler(Looper.getMainLooper()))
            MinimalLog.i(TAG, "Registered LauncherApps callback with Room persistence")
        } catch (e: Exception) {
            MinimalLog.e(TAG, "Failed to register LauncherApps callback", e)
        }
    }

    override fun getAllApps(): Flow<List<InstalledApp>> {
        return installedAppDao.observeAllApps().map { list ->
            list.map { it.toModel() }
        }.distinctUntilChanged()
    }

    override fun getAlwaysAvailableApps(): Flow<List<InstalledApp>> {
        return installedAppDao.observeAppsByCategory(AppCategory.ESSENTIAL).map { list ->
            list.map { it.toModel() }
        }.distinctUntilChanged()
    }

    override fun getManagedApps(): Flow<List<InstalledApp>> {
        return installedAppDao.observeAppsByCategory(AppCategory.MANAGED).map { list ->
            list.map { it.toModel() }
        }.distinctUntilChanged()
    }

    override fun getHomeApps(): Flow<List<InstalledApp>> {
        return installedAppDao.observeHomeApps().map { list ->
            list.map { it.toModel() }
        }.distinctUntilChanged()
    }

    override suspend fun getApp(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        val entity = installedAppDao.getApp(packageName)
        entity?.toModel() ?: appScanner.getAppDetails(packageName)
    }

    override suspend fun syncInstalledApps() = withContext(Dispatchers.IO) {
        val discoveredApps = appScanner.scanAllLaunchableApps()
        val entities = discoveredApps.map { app ->
            // Check if user has an existing override in app_rules
            val existingRule = appRuleDao.getRule(app.packageName)
            if (existingRule != null) {
                InstalledAppEntity(
                    packageName = app.packageName,
                    activityName = app.activityName,
                    label = existingRule.customLabel ?: app.label,
                    category = existingRule.category,
                    isSystemApp = app.isSystemApp,
                    isEssential = (existingRule.category == AppCategory.ESSENTIAL),
                    isBlockedInFocus = existingRule.isBlockedInFocus,
                    isFavoriteOnHome = existingRule.isFavoriteOnHome,
                    iconKey = app.iconKey,
                    lastUsedTimestamp = app.lastUsedTimestamp,
                    installedTimestamp = app.installedTimestamp,
                    classificationSource = existingRule.classificationSource
                )
            } else {
                app.toEntity()
            }
        }

        installedAppDao.upsertApps(entities)
        val packageNames = entities.map { it.packageName }
        installedAppDao.deleteAppsNotIn(packageNames)
        MinimalLog.i(TAG, "Synced ${entities.size} apps into Room database")
    }

    override suspend fun updateAppCategory(packageName: String, category: AppCategory) = withContext(Dispatchers.IO) {
        val isEssential = (category == AppCategory.ESSENTIAL)
        val isBlocked = (category == AppCategory.MANAGED)
        val currentApp = installedAppDao.getApp(packageName)

        // Persist rule override
        val rule = AppRuleEntity(
            packageName = packageName,
            category = category,
            isAlwaysAvailable = isEssential,
            isBlockedInFocus = isBlocked,
            isFavoriteOnHome = currentApp?.isFavoriteOnHome ?: isEssential,
            classificationSource = ClassificationSource.USER_OVERRIDE
        )
        appRuleDao.upsertRule(rule)

        // Update active installed app table
        installedAppDao.updateCategory(
            packageName = packageName,
            category = category,
            isEssential = isEssential,
            isBlocked = isBlocked
        )
        MinimalLog.i(TAG, "Persisted app category override in Room: $packageName -> $category")
    }

    override suspend fun toggleFavoriteOnHome(packageName: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        installedAppDao.updateFavorite(packageName, isFavorite)

        val currentRule = appRuleDao.getRule(packageName)
        val currentApp = installedAppDao.getApp(packageName)
        if (currentApp != null) {
            val rule = AppRuleEntity(
                packageName = packageName,
                category = currentApp.category,
                isAlwaysAvailable = currentApp.isEssential,
                isBlockedInFocus = currentApp.isBlockedInFocus,
                isFavoriteOnHome = isFavorite,
                classificationSource = currentRule?.classificationSource ?: ClassificationSource.USER_OVERRIDE
            )
            appRuleDao.upsertRule(rule)
        }
    }

    override suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        val app = appScanner.getAppDetails(packageName)
        if (app != null) {
            val rule = appRuleDao.getRule(packageName)
            val entity = if (rule != null) {
                app.toEntity().copy(
                    category = rule.category,
                    isEssential = rule.isAlwaysAvailable,
                    isBlockedInFocus = rule.isBlockedInFocus,
                    isFavoriteOnHome = rule.isFavoriteOnHome,
                    classificationSource = rule.classificationSource
                )
            } else {
                app.toEntity()
            }
            installedAppDao.upsertApp(entity)
            MinimalLog.i(TAG, "Persisted newly added package to Room: ${app.label} ($packageName)")
        }
    }

    override suspend fun onPackageRemoved(packageName: String) = withContext(Dispatchers.IO) {
        installedAppDao.deleteApp(packageName)
        MinimalLog.i(TAG, "Deleted removed package from Room: $packageName")
    }

    override suspend fun onPackageChanged(packageName: String) = withContext(Dispatchers.IO) {
        onPackageAdded(packageName)
    }

    companion object {
        private const val TAG = "AppRepository"
    }
}

private fun InstalledApp.toEntity(): InstalledAppEntity = InstalledAppEntity(
    packageName = packageName,
    activityName = activityName,
    label = label,
    category = category,
    isSystemApp = isSystemApp,
    isEssential = isEssential,
    isBlockedInFocus = isBlockedInFocus,
    isFavoriteOnHome = isFavoriteOnHome,
    iconKey = iconKey,
    lastUsedTimestamp = lastUsedTimestamp,
    installedTimestamp = installedTimestamp,
    classificationSource = ClassificationSource.AUTOMATIC_DEFAULT
)

private fun InstalledAppEntity.toModel(): InstalledApp = InstalledApp(
    packageName = packageName,
    activityName = activityName,
    label = label,
    category = category,
    isSystemApp = isSystemApp,
    isEssential = isEssential,
    isBlockedInFocus = isBlockedInFocus,
    isFavoriteOnHome = isFavoriteOnHome,
    iconKey = iconKey,
    lastUsedTimestamp = lastUsedTimestamp,
    installedTimestamp = installedTimestamp
)
