package com.minimalphone.core.domain

import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSettingsRepository : SettingsRepository {
    val onboardingFlow = MutableStateFlow(false)
    val adaptiveFrictionFlow = MutableStateFlow(true)
    val defaultDurationFlow = MutableStateFlow(25)
    val localAnalyticsFlow = MutableStateFlow(true)
    val appThemeFlow = MutableStateFlow(AppTheme.PURE_BLACK)
    val dumbModeFlow = MutableStateFlow(false)
    val autoGrayscaleFlow = MutableStateFlow(false)

    override val isOnboardingCompleted: Flow<Boolean> = onboardingFlow
    override val isAdaptiveFrictionEnabled: Flow<Boolean> = adaptiveFrictionFlow
    override val defaultFocusDurationMinutes: Flow<Int> = defaultDurationFlow
    override val isLocalAnalyticsEnabled: Flow<Boolean> = localAnalyticsFlow
    override val appTheme: Flow<AppTheme> = appThemeFlow
    override val isDumbModeEnabled: Flow<Boolean> = dumbModeFlow
    override val isAutoGrayscaleInFocusEnabled: Flow<Boolean> = autoGrayscaleFlow

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingFlow.value = completed
    }

    override suspend fun setAdaptiveFrictionEnabled(enabled: Boolean) {
        adaptiveFrictionFlow.value = enabled
    }

    override suspend fun setDefaultFocusDurationMinutes(minutes: Int) {
        defaultDurationFlow.value = minutes
    }

    override suspend fun setLocalAnalyticsEnabled(enabled: Boolean) {
        localAnalyticsFlow.value = enabled
    }

    override suspend fun setAppTheme(theme: AppTheme) {
        appThemeFlow.value = theme
    }

    override suspend fun setDumbModeEnabled(enabled: Boolean) {
        dumbModeFlow.value = enabled
    }

    override suspend fun setAutoGrayscaleInFocusEnabled(enabled: Boolean) {
        autoGrayscaleFlow.value = enabled
    }
}

class SettingsUseCasesTest {

    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var setOnboardingCompletedUseCase: SetOnboardingCompletedUseCase
    private lateinit var updateAdaptiveFrictionUseCase: UpdateAdaptiveFrictionUseCase
    private lateinit var updateDefaultFocusDurationUseCase: UpdateDefaultFocusDurationUseCase
    private lateinit var updateLocalAnalyticsUseCase: UpdateLocalAnalyticsUseCase
    private lateinit var updateAppThemeUseCase: UpdateAppThemeUseCase

    @Before
    fun setUp() {
        settingsRepository = FakeSettingsRepository()
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
        setOnboardingCompletedUseCase = SetOnboardingCompletedUseCase(settingsRepository)
        updateAdaptiveFrictionUseCase = UpdateAdaptiveFrictionUseCase(settingsRepository)
        updateDefaultFocusDurationUseCase = UpdateDefaultFocusDurationUseCase(settingsRepository)
        updateLocalAnalyticsUseCase = UpdateLocalAnalyticsUseCase(settingsRepository)
        updateAppThemeUseCase = UpdateAppThemeUseCase(settingsRepository)
    }

    @Test
    fun getSettings_combinesAllPreferenceFlows() = runTest {
        val initial = getSettingsUseCase().first()
        assertFalse(initial.isOnboardingCompleted)
        assertTrue(initial.isAdaptiveFrictionEnabled)
        assertEquals(25, initial.defaultFocusDurationMinutes)
        assertTrue(initial.isLocalAnalyticsEnabled)
        assertEquals(AppTheme.PURE_BLACK, initial.appTheme)
    }

    @Test
    fun setOnboardingCompleted_updatesFlow() = runTest {
        setOnboardingCompletedUseCase(true)
        val updated = getSettingsUseCase().first()
        assertTrue(updated.isOnboardingCompleted)
    }

    @Test
    fun updateFocusDuration_updatesFlow() = runTest {
        updateDefaultFocusDurationUseCase(45)
        val updated = getSettingsUseCase().first()
        assertEquals(45, updated.defaultFocusDurationMinutes)
    }

    @Test
    fun updateAppTheme_updatesFlow() = runTest {
        updateAppThemeUseCase(AppTheme.E_INK_PAPER)
        val updated = getSettingsUseCase().first()
        assertEquals(AppTheme.E_INK_PAPER, updated.appTheme)
    }

    @Test
    fun updateAutoGrayscaleInFocus_updatesFlow() = runTest {
        val useCase = UpdateAutoGrayscaleInFocusUseCase(settingsRepository)
        useCase(true)
        val updated = getSettingsUseCase().first()
        assertTrue(updated.isAutoGrayscaleInFocusEnabled)
    }
}
