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
    val settingsGuardModeFlow = MutableStateFlow(com.minimalphone.core.model.SettingsGuardMode.OFF)
    val quarantineHoursFlow = MutableStateFlow(com.minimalphone.core.model.QuarantineHours.TWENTY_FOUR)
    val uninstallLockFlow = MutableStateFlow(false)
    val pauseGlobalFlow = MutableStateFlow(true)
    val defaultPauseSecondsFlow = MutableStateFlow(5)
    val defaultSessionMinutesFlow = MutableStateFlow(10)
    val defaultPauseStyleFlow = MutableStateFlow(com.minimalphone.core.model.PauseStyle.BREATHE)
    val progressiveDelayFlow = MutableStateFlow(false)
    val gestureConfigFlow = MutableStateFlow(com.minimalphone.core.model.GestureAction.defaultGestureConfig())
    val searchAutoLaunchModeFlow = MutableStateFlow(com.minimalphone.core.model.AutoLaunchMode.ESSENTIAL_AND_PINNED)
    val searchFuzzyEnabledFlow = MutableStateFlow(true)
    val gestureHintShownFlow = MutableStateFlow(false)

    override val isOnboardingCompleted: Flow<Boolean> = onboardingFlow
    override val isAdaptiveFrictionEnabled: Flow<Boolean> = adaptiveFrictionFlow
    override val defaultFocusDurationMinutes: Flow<Int> = defaultDurationFlow
    override val isLocalAnalyticsEnabled: Flow<Boolean> = localAnalyticsFlow
    override val appTheme: Flow<AppTheme> = appThemeFlow
    override val isDumbModeEnabled: Flow<Boolean> = dumbModeFlow
    override val isAutoGrayscaleInFocusEnabled: Flow<Boolean> = autoGrayscaleFlow
    override val settingsGuardMode: Flow<com.minimalphone.core.model.SettingsGuardMode> = settingsGuardModeFlow
    override val quarantineHours: Flow<com.minimalphone.core.model.QuarantineHours> = quarantineHoursFlow
    override val uninstallLockEnabled: Flow<Boolean> = uninstallLockFlow
    override val pauseGlobalEnabled: Flow<Boolean> = pauseGlobalFlow
    override val defaultPauseSeconds: Flow<Int> = defaultPauseSecondsFlow
    override val defaultSessionMinutes: Flow<Int> = defaultSessionMinutesFlow
    override val defaultPauseStyle: Flow<com.minimalphone.core.model.PauseStyle> = defaultPauseStyleFlow
    override val progressiveDelayEnabled: Flow<Boolean> = progressiveDelayFlow
    override val gestureConfig: Flow<Map<com.minimalphone.core.model.GestureType, com.minimalphone.core.model.GestureAction>> = gestureConfigFlow
    override val searchAutoLaunchMode: Flow<com.minimalphone.core.model.AutoLaunchMode> = searchAutoLaunchModeFlow
    override val searchFuzzyEnabled: Flow<Boolean> = searchFuzzyEnabledFlow
    override val gestureHintShown: Flow<Boolean> = gestureHintShownFlow

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

    override suspend fun setSettingsGuardMode(mode: com.minimalphone.core.model.SettingsGuardMode) {
        settingsGuardModeFlow.value = mode
    }

    override suspend fun setQuarantineHours(hours: com.minimalphone.core.model.QuarantineHours) {
        quarantineHoursFlow.value = hours
    }

    override suspend fun setUninstallLockEnabled(enabled: Boolean) {
        uninstallLockFlow.value = enabled
    }

    override suspend fun setPauseGlobalEnabled(enabled: Boolean) {
        pauseGlobalFlow.value = enabled
    }

    override suspend fun setDefaultPauseSeconds(seconds: Int) {
        defaultPauseSecondsFlow.value = seconds
    }

    override suspend fun setDefaultSessionMinutes(minutes: Int) {
        defaultSessionMinutesFlow.value = minutes
    }

    override suspend fun setDefaultPauseStyle(style: com.minimalphone.core.model.PauseStyle) {
        defaultPauseStyleFlow.value = style
    }

    override suspend fun setProgressiveDelayEnabled(enabled: Boolean) {
        progressiveDelayFlow.value = enabled
    }

    override suspend fun setGestureAction(type: com.minimalphone.core.model.GestureType, action: com.minimalphone.core.model.GestureAction) {
        val map = gestureConfigFlow.value.toMutableMap()
        map[type] = action
        gestureConfigFlow.value = map
    }

    override suspend fun setGestureConfig(config: Map<com.minimalphone.core.model.GestureType, com.minimalphone.core.model.GestureAction>) {
        gestureConfigFlow.value = config
    }

    override suspend fun setSearchAutoLaunchMode(mode: com.minimalphone.core.model.AutoLaunchMode) {
        searchAutoLaunchModeFlow.value = mode
    }

    override suspend fun setSearchFuzzyEnabled(enabled: Boolean) {
        searchFuzzyEnabledFlow.value = enabled
    }

    override suspend fun setGestureHintShown(shown: Boolean) {
        gestureHintShownFlow.value = shown
    }
}

class SettingsUseCasesTest {

    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var getSettingsUseCase: GetSettingsUseCase

    @Before
    fun setUp() {
        settingsRepository = FakeSettingsRepository()
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
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
        settingsRepository.setOnboardingCompleted(true)
        val updated = getSettingsUseCase().first()
        assertTrue(updated.isOnboardingCompleted)
    }

    @Test
    fun updateFocusDuration_updatesFlow() = runTest {
        settingsRepository.setDefaultFocusDurationMinutes(45)
        val updated = getSettingsUseCase().first()
        assertEquals(45, updated.defaultFocusDurationMinutes)
    }

    @Test
    fun updateAppTheme_updatesFlow() = runTest {
        settingsRepository.setAppTheme(AppTheme.E_INK_PAPER)
        val updated = getSettingsUseCase().first()
        assertEquals(AppTheme.E_INK_PAPER, updated.appTheme)
    }

    @Test
    fun updateAutoGrayscaleInFocus_updatesFlow() = runTest {
        settingsRepository.setAutoGrayscaleInFocusEnabled(true)
        val updated = getSettingsUseCase().first()
        assertTrue(updated.isAutoGrayscaleInFocusEnabled)
    }
}
