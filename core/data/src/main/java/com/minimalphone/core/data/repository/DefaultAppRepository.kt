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
import com.minimalphone.core.data.defaults.DefaultsBootstrapper
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
import kotlinx.coroutines.flow.first
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
    private val appRuleDao: AppRuleDao,
    private val defaultsBootstrapper: DefaultsBootstrapper,
    private val settingsRepository: SettingsRepository
) : AppRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps: LauncherApps? = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps

    private val launcherCallback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps callback: onPackageAdded ($packageName, user: $user)")
            scope.launch { this@DefaultAppRepository.onPackageAdded(packageName) }
        }

        override fun onPackageRemoved(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps callback: onPackageRemoved ($packageName, user: $user)")
            scope.launch { this@DefaultAppRepository.onPackageRemoved(packageName) }
        }

        override fun onPackageChanged(packageName: String, user: UserHandle) {
            MinimalLog.i(TAG, "LauncherApps callback: onPackageChanged ($packageName, user: $user)")
            scope.launch { this@DefaultAppRepository.onPackageChanged(packageName) }
        }

        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { syncInstalledApps() }
        }

        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) {
            scope.launch { syncInstalledApps() }
        }
    }

    init {
        registerLauncherCallback()
        scope.launch {
            syncInstalledApps()
            // Pin the 6 essential apps to home on first install only
            defaultsBootstrapper.bootstrapIfNeeded()
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
            val familyId = existingRule?.familyId
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
                    classificationSource = existingRule.classificationSource,
                    familyId = familyId,
                    quarantineUntil = existingRule.quarantineUntil,
                    isClone = app.isClone
                )
            } else {
                app.toEntity().copy(familyId = familyId)
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
        val currentRule = appRuleDao.getRule(packageName)
        val familyId = currentRule?.familyId

        // Persist rule override
        val rule = AppRuleEntity(
            packageName = packageName,
            category = category,
            isAlwaysAvailable = isEssential,
            isBlockedInFocus = isBlocked,
            isFavoriteOnHome = currentApp?.isFavoriteOnHome ?: isEssential,
            classificationSource = ClassificationSource.USER_OVERRIDE,
            familyId = familyId,
            quarantineUntil = currentRule?.quarantineUntil
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
        val familyId = currentRule?.familyId
        if (currentApp != null) {
            val rule = AppRuleEntity(
                packageName = packageName,
                category = currentApp.category,
                isAlwaysAvailable = currentApp.isEssential,
                isBlockedInFocus = currentApp.isBlockedInFocus,
                isFavoriteOnHome = isFavorite,
                classificationSource = currentRule?.classificationSource ?: ClassificationSource.USER_OVERRIDE,
                familyId = familyId,
                quarantineUntil = currentRule?.quarantineUntil
            )
            appRuleDao.upsertRule(rule)
        }
    }

    override suspend fun onPackageAdded(packageName: String) = withContext(Dispatchers.IO) {
        val app = appScanner.getAppDetails(packageName)
        if (app != null) {
            val existingRule = appRuleDao.getRule(packageName)
            val familyId = existingRule?.familyId

            val resolvedRule = if (existingRule != null) {
                existingRule
            } else {
                // New app — evaluate quarantine (M21)
                val isEssential = AppScanner.isEssentialPackage(packageName, app.label)
                val qHours = settingsRepository.quarantineHours.first()
                val qUntil = if (!isEssential && qHours.hours != null) {
                    System.currentTimeMillis() + qHours.hours!! * 60L * 60L * 1000L
                } else null

                val newRule = AppRuleEntity(
                    packageName = packageName,
                    category = if (isEssential) AppCategory.ESSENTIAL else AppCategory.MANAGED,
                    isAlwaysAvailable = isEssential,
                    isBlockedInFocus = !isEssential,
                    isFavoriteOnHome = isEssential,
                    classificationSource = ClassificationSource.AUTOMATIC_DEFAULT,
                    familyId = familyId,
                    quarantineUntil = qUntil
                )
                appRuleDao.upsertRule(newRule)
                newRule
            }

            val entity = app.toEntity().copy(
                category = resolvedRule.category,
                isEssential = resolvedRule.isAlwaysAvailable,
                isBlockedInFocus = resolvedRule.isBlockedInFocus,
                isFavoriteOnHome = resolvedRule.isFavoriteOnHome,
                classificationSource = resolvedRule.classificationSource,
                familyId = familyId,
                quarantineUntil = resolvedRule.quarantineUntil,
                isClone = app.isClone
            )
            installedAppDao.upsertApp(entity)
            MinimalLog.i(TAG, "Persisted newly added package to Room: ${app.label} ($packageName)")
        }
    }

    override suspend fun onPackageRemoved(packageName: String) = withContext(Dispatchers.IO) {
        // We delete from installed_apps, but PRESERVE app_rules & time limits across uninstalls (M21)
        installedAppDao.deleteApp(packageName)
        MinimalLog.i(TAG, "Deleted removed package from Room installed_apps: $packageName")
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
    classificationSource = ClassificationSource.AUTOMATIC_DEFAULT,
    familyId = familyId,
    quarantineUntil = quarantineUntil,
    isClone = isClone
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
    installedTimestamp = installedTimestamp,
    familyId = familyId,
    quarantineUntil = quarantineUntil,
    isClone = isClone
)
