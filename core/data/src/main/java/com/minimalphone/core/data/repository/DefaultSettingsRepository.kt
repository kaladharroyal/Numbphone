package com.minimalphone.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.minimalphone.core.model.AutoLaunchMode
import com.minimalphone.core.model.GestureAction
import com.minimalphone.core.model.GestureType
import com.minimalphone.core.model.QuarantineHours
import com.minimalphone.core.model.SettingsGuardMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("minimal_phone_settings")
        }
    }

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val ADAPTIVE_FRICTION_ENABLED = booleanPreferencesKey("adaptive_friction_enabled")
        val DEFAULT_FOCUS_DURATION = intPreferencesKey("default_focus_duration")
        val LOCAL_ANALYTICS_ENABLED = booleanPreferencesKey("local_analytics_enabled")
        val APP_THEME = stringPreferencesKey("app_theme")
        val DUMB_MODE_ENABLED = booleanPreferencesKey("dumb_mode_enabled")
        val AUTO_GRAYSCALE_IN_FOCUS = booleanPreferencesKey("auto_grayscale_in_focus")
        val SETTINGS_GUARD_MODE = stringPreferencesKey("settings_guard_mode")
        val QUARANTINE_HOURS = stringPreferencesKey("quarantine_hours")
        val UNINSTALL_LOCK_ENABLED = booleanPreferencesKey("uninstall_lock_enabled")
        val PAUSE_GLOBAL_ENABLED = booleanPreferencesKey("pause_global_enabled")
        val DEFAULT_PAUSE_SECONDS = intPreferencesKey("default_pause_seconds")
        val DEFAULT_SESSION_MINUTES = intPreferencesKey("default_session_minutes")
        val DEFAULT_PAUSE_STYLE = stringPreferencesKey("default_pause_style")
        val PROGRESSIVE_DELAY_ENABLED = booleanPreferencesKey("progressive_delay_enabled")
        val GESTURE_CONFIG = stringPreferencesKey("gesture_config")
        val SEARCH_AUTO_LAUNCH_MODE = stringPreferencesKey("search_auto_launch_mode")
        val SEARCH_FUZZY_ENABLED = booleanPreferencesKey("search_fuzzy_enabled")
        val GESTURE_HINT_SHOWN = booleanPreferencesKey("gesture_hint_shown")
    }

    override val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    override val isAdaptiveFrictionEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.ADAPTIVE_FRICTION_ENABLED] ?: true
        }

    override val defaultFocusDurationMinutes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_FOCUS_DURATION] ?: 25
        }

    override val isLocalAnalyticsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LOCAL_ANALYTICS_ENABLED] ?: true
        }

    override val appTheme: Flow<com.minimalphone.core.model.AppTheme> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            com.minimalphone.core.model.AppTheme.fromName(preferences[PreferencesKeys.APP_THEME])
        }

    override val isDumbModeEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DUMB_MODE_ENABLED] ?: false
        }

    override val isAutoGrayscaleInFocusEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_GRAYSCALE_IN_FOCUS] ?: false
        }

    override val settingsGuardMode: Flow<SettingsGuardMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.SETTINGS_GUARD_MODE]
            try {
                if (value != null) SettingsGuardMode.valueOf(value) else SettingsGuardMode.OFF
            } catch (_: Exception) {
                SettingsGuardMode.OFF
            }
        }

    override val quarantineHours: Flow<QuarantineHours> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.QUARANTINE_HOURS]
            try {
                if (value != null) QuarantineHours.valueOf(value) else QuarantineHours.TWENTY_FOUR
            } catch (_: Exception) {
                QuarantineHours.TWENTY_FOUR
            }
        }

    override val uninstallLockEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.UNINSTALL_LOCK_ENABLED] ?: false
        }

    override val pauseGlobalEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.PAUSE_GLOBAL_ENABLED] ?: true
        }

    override val defaultPauseSeconds: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_PAUSE_SECONDS] ?: 5
        }

    override val defaultSessionMinutes: Flow<Int> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEFAULT_SESSION_MINUTES] ?: 10
        }

    override val defaultPauseStyle: Flow<com.minimalphone.core.model.PauseStyle> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            com.minimalphone.core.model.PauseStyle.fromName(preferences[PreferencesKeys.DEFAULT_PAUSE_STYLE])
        }

    override val progressiveDelayEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.PROGRESSIVE_DELAY_ENABLED] ?: false
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    override suspend fun setAdaptiveFrictionEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADAPTIVE_FRICTION_ENABLED] = enabled
        }
    }

    override suspend fun setDefaultFocusDurationMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_FOCUS_DURATION] = minutes.coerceIn(5, 720)
        }
    }

    override suspend fun setLocalAnalyticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCAL_ANALYTICS_ENABLED] = enabled
        }
    }

    override suspend fun setAppTheme(theme: com.minimalphone.core.model.AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name
        }
    }

    override suspend fun setDumbModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DUMB_MODE_ENABLED] = enabled
        }
    }

    override suspend fun setAutoGrayscaleInFocusEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_GRAYSCALE_IN_FOCUS] = enabled
        }
    }

    override suspend fun setSettingsGuardMode(mode: SettingsGuardMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SETTINGS_GUARD_MODE] = mode.name
        }
    }

    override suspend fun setQuarantineHours(hours: QuarantineHours) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUARANTINE_HOURS] = hours.name
        }
    }

    override suspend fun setUninstallLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UNINSTALL_LOCK_ENABLED] = enabled
        }
    }

    override suspend fun setPauseGlobalEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAUSE_GLOBAL_ENABLED] = enabled
        }
    }

    override suspend fun setDefaultPauseSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PAUSE_SECONDS] = seconds
        }
    }

    override suspend fun setDefaultSessionMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SESSION_MINUTES] = minutes
        }
    }

    override suspend fun setDefaultPauseStyle(style: com.minimalphone.core.model.PauseStyle) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_PAUSE_STYLE] = style.name
        }
    }

    override suspend fun setProgressiveDelayEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROGRESSIVE_DELAY_ENABLED] = enabled
        }
    }

    override val gestureConfig: Flow<Map<GestureType, GestureAction>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val serialized = preferences[PreferencesKeys.GESTURE_CONFIG] ?: ""
            GestureAction.deserializeMap(serialized)
        }

    override val searchAutoLaunchMode: Flow<AutoLaunchMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SEARCH_AUTO_LAUNCH_MODE]?.let {
                runCatching { AutoLaunchMode.valueOf(it) }.getOrNull()
            } ?: AutoLaunchMode.ESSENTIAL_AND_PINNED
        }

    override val searchFuzzyEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.SEARCH_FUZZY_ENABLED] ?: true
        }

    override val gestureHintShown: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.GESTURE_HINT_SHOWN] ?: false
        }

    override suspend fun setGestureAction(type: GestureType, action: GestureAction) {
        dataStore.edit { preferences ->
            val serialized = preferences[PreferencesKeys.GESTURE_CONFIG] ?: ""
            val currentMap = GestureAction.deserializeMap(serialized).toMutableMap()
            currentMap[type] = action
            preferences[PreferencesKeys.GESTURE_CONFIG] = GestureAction.serializeMap(currentMap)
        }
    }

    override suspend fun setGestureConfig(config: Map<GestureType, GestureAction>) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_CONFIG] = GestureAction.serializeMap(config)
        }
    }

    override suspend fun setSearchAutoLaunchMode(mode: AutoLaunchMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_AUTO_LAUNCH_MODE] = mode.name
        }
    }

    override suspend fun setSearchFuzzyEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SEARCH_FUZZY_ENABLED] = enabled
        }
    }

    override suspend fun setGestureHintShown(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GESTURE_HINT_SHOWN] = shown
        }
    }
}
