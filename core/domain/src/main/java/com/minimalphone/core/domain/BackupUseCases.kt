package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.EssentialAppRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.ScheduleRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSchedule
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime
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
    private val settingsRepository: SettingsRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val scheduleRepository: ScheduleRepository,
    private val essentialAppRepository: EssentialAppRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(): BackupExportResult {
        val apps = appRepository.getAllApps().first()
        val sessions = focusSessionRepository.getAllSessions().first()
        val onboarding = settingsRepository.isOnboardingCompleted.first()
        val friction = settingsRepository.isAdaptiveFrictionEnabled.first()
        val duration = settingsRepository.defaultFocusDurationMinutes.first()
        val analytics = settingsRepository.isLocalAnalyticsEnabled.first()
        val theme = settingsRepository.appTheme.first()
        val dumbMode = settingsRepository.isDumbModeEnabled.first()

        val timeLimits = appTimeLimitRepository.observeAllLimits().first()
        val schedules = scheduleRepository.observeSchedules().first()
        val essentials = essentialAppRepository.observeEssentialApps().first()
        val contacts = contactRepository.observeAllContacts().first()

        val root = JSONObject()
        root.put("version", 2)
        root.put("timestamp", System.currentTimeMillis())

        val settingsObj = JSONObject()
        settingsObj.put("isOnboardingCompleted", onboarding)
        settingsObj.put("isAdaptiveFrictionEnabled", friction)
        settingsObj.put("defaultFocusDurationMinutes", duration)
        settingsObj.put("isLocalAnalyticsEnabled", analytics)
        settingsObj.put("appTheme", theme.name)
        settingsObj.put("isDumbModeEnabled", dumbMode)
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

        // Time limits
        val limitsArray = JSONArray()
        timeLimits.forEach { limit ->
            val lObj = JSONObject()
            lObj.put("packageName", limit.packageName)
            lObj.put("dailyLimitMinutes", limit.dailyLimitMinutes)
            lObj.put("isEnabled", limit.isEnabled)
            limitsArray.put(lObj)
        }
        root.put("timeLimits", limitsArray)

        // Schedules (M15)
        val schedulesArray = JSONArray()
        schedules.forEach { sc ->
            val scObj = JSONObject()
            scObj.put("id", sc.id)
            scObj.put("title", sc.title)
            scObj.put("daysOfWeek", sc.daysOfWeek.joinToString(",") { it.name })
            scObj.put("startTimeHour", sc.startTime.hour)
            scObj.put("startTimeMinute", sc.startTime.minute)
            scObj.put("durationMinutes", sc.durationMinutes)
            scObj.put("mode", sc.mode.name)
            scObj.put("isEnabled", sc.isEnabled)
            schedulesArray.put(scObj)
        }
        root.put("schedules", schedulesArray)

        // Essential apps (M12)
        val essentialsArray = JSONArray()
        essentials.forEach { e ->
            val eObj = JSONObject()
            eObj.put("packageName", e.packageName)
            eObj.put("label", e.label)
            eObj.put("isSystemDefault", e.isSystemDefault)
            essentialsArray.put(eObj)
        }
        root.put("essentialApps", essentialsArray)

        // Quick Contacts (M20)
        val contactsArray = JSONArray()
        contacts.forEach { c ->
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            cObj.put("phoneNumber", c.phoneNumber)
            cObj.put("isPinned", c.isPinned)
            contactsArray.put(cObj)
        }
        root.put("quickContacts", contactsArray)

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
    private val settingsRepository: SettingsRepository,
    private val appTimeLimitRepository: AppTimeLimitRepository,
    private val scheduleRepository: ScheduleRepository,
    private val essentialAppRepository: EssentialAppRepository,
    private val contactRepository: ContactRepository
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
            if (settingsObj.has("isDumbModeEnabled")) {
                settingsRepository.setDumbModeEnabled(settingsObj.getBoolean("isDumbModeEnabled"))
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
                    val statusName = sObj.optString("status", SessionStatus.COMPLETED.name)
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
                        SessionStatus.valueOf(statusName)
                    } catch (e: Exception) {
                        SessionStatus.COMPLETED
                    }

                    val session = FocusSession(
                        id = id,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = durationMinutes,
                        goal = FocusGoal(
                            id = UUID.randomUUID().toString(),
                            title = goalTitle
                        ),
                        mode = mode,
                        status = status,
                        bypassAttemptsCount = bypassCount,
                        exitFrictionSecondsCompleted = frictionSecs
                    )
                    focusSessionRepository.startSession(session)
                    if (status == SessionStatus.COMPLETED) {
                        focusSessionRepository.completeSession(id)
                    }
                    restoredSessions++
                }
            }

            // 4. Restore Time Limits (supports "timeLimits" or legacy "budgets")
            if (root.has("timeLimits")) {
                val limitsArray = root.getJSONArray("timeLimits")
                for (i in 0 until limitsArray.length()) {
                    val lObj = limitsArray.getJSONObject(i)
                    val pkg = lObj.getString("packageName")
                    val limit = lObj.optInt("dailyLimitMinutes", 30)
                    val isEnabled = lObj.optBoolean("isEnabled", true)
                    appTimeLimitRepository.setLimit(pkg, limit, isEnabled)
                }
            } else if (root.has("budgets")) {
                val budgetsArray = root.getJSONArray("budgets")
                for (i in 0 until budgetsArray.length()) {
                    val bObj = budgetsArray.getJSONObject(i)
                    val pkg = bObj.getString("packageName")
                    val limit = bObj.optInt("dailyLimitMinutes", 30)
                    val isEnabled = bObj.optBoolean("enabled", true)
                    appTimeLimitRepository.setLimit(pkg, limit, isEnabled)
                }
            }

            // 5. Restore Schedules
            if (root.has("schedules")) {
                val schedulesArray = root.getJSONArray("schedules")
                for (i in 0 until schedulesArray.length()) {
                    val scObj = schedulesArray.getJSONObject(i)
                    val id = scObj.optString("id", UUID.randomUUID().toString())
                    val title = scObj.getString("title")
                    val daysStr = scObj.optString("daysOfWeek", "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                    val days = daysStr.split(",")
                        .mapNotNull { runCatching { DayOfWeek.valueOf(it.trim()) }.getOrNull() }
                        .toSet()
                    val hour = scObj.optInt("startTimeHour", 9)
                    val minute = scObj.optInt("startTimeMinute", 0)
                    val duration = scObj.optInt("durationMinutes", 60)
                    val mode = runCatching { FocusMode.valueOf(scObj.optString("mode", FocusMode.STRICT.name)) }.getOrDefault(FocusMode.STRICT)
                    val enabled = scObj.optBoolean("isEnabled", true)

                    scheduleRepository.saveSchedule(
                        FocusSchedule(
                            id = id,
                            title = title,
                            daysOfWeek = if (days.isEmpty()) DayOfWeek.values().toSet() else days,
                            startTime = LocalTime.of(hour, minute),
                            durationMinutes = duration,
                            mode = mode,
                            isEnabled = enabled
                        )
                    )
                }
            }

            // 6. Restore Quick Contacts
            if (root.has("quickContacts")) {
                val contactsArray = root.getJSONArray("quickContacts")
                for (i in 0 until contactsArray.length()) {
                    val cObj = contactsArray.getJSONObject(i)
                    val name = cObj.getString("name")
                    val phone = cObj.getString("phoneNumber")
                    val isPinned = cObj.optBoolean("isPinned", true)
                    contactRepository.saveContact(name, phone, isPinned)
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
