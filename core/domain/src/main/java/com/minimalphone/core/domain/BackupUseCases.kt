package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

data class BackupExportResult(
    val jsonString: String,
    val totalApps: Int,
    val totalSessions: Int,
    val timestamp: Long
)

data class BackupImportResult(
    val isSuccess: Boolean,
    val restoredAppsCount: Int,
    val restoredSessionsCount: Int,
    val errorMessage: String? = null
)

class ExportBackupJsonUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(): BackupExportResult {
        val apps = appRepository.getAllApps().first()
        val sessions = focusSessionRepository.getAllSessions().first()
        val onboarding = settingsRepository.isOnboardingCompleted.first()
        val friction = settingsRepository.isAdaptiveFrictionEnabled.first()
        val duration = settingsRepository.defaultFocusDurationMinutes.first()
        val analytics = settingsRepository.isLocalAnalyticsEnabled.first()
        val theme = settingsRepository.appTheme.first()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("isOnboardingCompleted", onboarding)
        settingsObj.put("isAdaptiveFrictionEnabled", friction)
        settingsObj.put("defaultFocusDurationMinutes", duration)
        settingsObj.put("isLocalAnalyticsEnabled", analytics)
        settingsObj.put("appTheme", theme.name)
        root.put("settings", settingsObj)

        val appsArray = JSONArray()
        apps.forEach { app ->
            val appObj = JSONObject()
            appObj.put("packageName", app.packageName)
            appObj.put("category", app.category.name)
            appObj.put("isFavoriteOnHome", app.isFavoriteOnHome)
            appsArray.put(appObj)
        }
        root.put("apps", appsArray)

        val sessionsArray = JSONArray()
        sessions.forEach { s ->
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("goalTitle", s.goal.title)
            sObj.put("durationMinutes", s.durationMinutes)
            sObj.put("mode", s.mode.name)
            sObj.put("status", s.status.name)
            sObj.put("startTime", s.startTime)
            sObj.put("endTime", s.endTime)
            sObj.put("exitFrictionSecondsCompleted", s.exitFrictionSecondsCompleted)
            sObj.put("bypassAttemptsCount", s.bypassAttemptsCount)
            sessionsArray.put(sObj)
        }
        root.put("focusSessions", sessionsArray)

        val jsonString = root.toString(2)
        return BackupExportResult(
            jsonString = jsonString,
            totalApps = apps.size,
            totalSessions = sessions.size,
            timestamp = root.getLong("timestamp")
        )
    }
}

class ImportBackupJsonUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(jsonString: String): BackupImportResult {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("settings")) {
                return BackupImportResult(false, 0, 0, "Invalid backup format: missing settings object")
            }

            // 1. Restore Settings
            val settingsObj = root.getJSONObject("settings")
            if (settingsObj.has("isOnboardingCompleted")) {
                settingsRepository.setOnboardingCompleted(settingsObj.getBoolean("isOnboardingCompleted"))
            }
            if (settingsObj.has("isAdaptiveFrictionEnabled")) {
                settingsRepository.setAdaptiveFrictionEnabled(settingsObj.getBoolean("isAdaptiveFrictionEnabled"))
            }
            if (settingsObj.has("defaultFocusDurationMinutes")) {
                settingsRepository.setDefaultFocusDurationMinutes(settingsObj.getInt("defaultFocusDurationMinutes"))
            }
            if (settingsObj.has("isLocalAnalyticsEnabled")) {
                settingsRepository.setLocalAnalyticsEnabled(settingsObj.getBoolean("isLocalAnalyticsEnabled"))
            }
            if (settingsObj.has("appTheme")) {
                val themeName = settingsObj.getString("appTheme")
                settingsRepository.setAppTheme(AppTheme.fromName(themeName))
            }

            // 2. Restore App Classifications & Favorites
            var restoredApps = 0
            if (root.has("apps")) {
                val appsArray = root.getJSONArray("apps")
                for (i in 0 until appsArray.length()) {
                    val appObj = appsArray.getJSONObject(i)
                    val pkg = appObj.getString("packageName")
                    val categoryName = appObj.optString("category", AppCategory.MANAGED.name)
                    val isFav = appObj.optBoolean("isFavoriteOnHome", false)

                    val category = try {
                        AppCategory.valueOf(categoryName)
                    } catch (e: Exception) {
                        AppCategory.MANAGED
                    }

                    appRepository.updateAppCategory(pkg, category)
                    appRepository.toggleFavoriteOnHome(pkg, isFav)
                    restoredApps++
                }
            }

            // 3. Restore Focus Sessions
            var restoredSessions = 0
            if (root.has("focusSessions")) {
                val sessionsArray = root.getJSONArray("focusSessions")
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.getJSONObject(i)
                    val id = sObj.optString("id", UUID.randomUUID().toString())
                    val goalTitle = sObj.optString("goalTitle", sObj.optString("goal", "Focus Session"))
                    val durationMinutes = sObj.optInt("durationMinutes", sObj.optInt("targetDurationMinutes", 25))
                    val modeName = sObj.optString("mode", FocusMode.STRICT.name)
                    val statusName = sObj.optString("status", com.minimalphone.core.model.SessionStatus.COMPLETED.name)
                    val startTime = sObj.optLong("startTime", System.currentTimeMillis())
                    val endTime = sObj.optLong("endTime", startTime + (durationMinutes * 60 * 1000L))
                    val frictionSecs = sObj.optInt("exitFrictionSecondsCompleted", 0)
                    val bypassCount = sObj.optInt("bypassAttemptsCount", 0)

                    val mode = try {
                        FocusMode.valueOf(modeName)
                    } catch (e: Exception) {
                        FocusMode.STRICT
                    }

                    val status = try {
                        com.minimalphone.core.model.SessionStatus.valueOf(statusName)
                    } catch (e: Exception) {
                        com.minimalphone.core.model.SessionStatus.COMPLETED
                    }

                    val session = FocusSession(
                        id = id,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = durationMinutes,
                        goal = com.minimalphone.core.model.FocusGoal(
                            id = UUID.randomUUID().toString(),
                            title = goalTitle
                        ),
                        mode = mode,
                        status = status,
                        bypassAttemptsCount = bypassCount,
                        exitFrictionSecondsCompleted = frictionSecs
                    )
                    focusSessionRepository.startSession(session)
                    if (status == com.minimalphone.core.model.SessionStatus.COMPLETED) {
                        focusSessionRepository.completeSession(id)
                    }
                    restoredSessions++
                }
            }

            BackupImportResult(
                isSuccess = true,
                restoredAppsCount = restoredApps,
                restoredSessionsCount = restoredSessions
            )
        } catch (e: Exception) {
            BackupImportResult(
                isSuccess = false,
                restoredAppsCount = 0,
                restoredSessionsCount = 0,
                errorMessage = e.localizedMessage ?: "Unknown parse error"
            )
        }
    }
}
