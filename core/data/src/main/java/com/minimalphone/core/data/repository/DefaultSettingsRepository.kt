package com.minimalphone.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
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
        val APP_THEME = androidx.datastore.preferences.core.stringPreferencesKey("app_theme")
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
}
