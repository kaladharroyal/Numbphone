package com.minimalphone.core.domain.rules

import com.minimalphone.core.data.repository.AppTimeLimitRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppLaunchDecision
import com.minimalphone.core.model.AppTheme
import com.minimalphone.core.model.AppTimeLimit
import com.minimalphone.core.model.DailyUsageSummary
import com.minimalphone.core.model.FocusGoal
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRuleTimeLimitRepo : AppTimeLimitRepository {
    val limits = mutableMapOf<String, AppTimeLimit>()

    override fun observeAllLimits(): Flow<List<AppTimeLimit>> = MutableStateFlow(limits.values.toList())
    override suspend fun getLimit(packageName: String): AppTimeLimit? = limits[packageName]
    override fun observeLimit(packageName: String): Flow<AppTimeLimit?> = MutableStateFlow(limits[packageName])
    override suspend fun setLimit(packageName: String, limitMinutes: Int, isEnabled: Boolean) {
        limits[packageName] = AppTimeLimit(packageName, limitMinutes, isEnabled)
    }
    override suspend fun addEmergencyExtension(packageName: String, additionalMinutes: Int) {
        val cur = limits[packageName] ?: AppTimeLimit(packageName, 0)
        limits[packageName] = cur.copy(emergencyExtensionMinutes = cur.emergencyExtensionMinutes + additionalMinutes)
    }
    override suspend fun removeLimit(packageName: String) {
        limits.remove(packageName)
    }
}

class FakeRuleUsageStatsRepo : UsageStatsRepository {
    val usageMap = mutableMapOf<String, Long>()
    override fun getDailyUsageSummary(): Flow<DailyUsageSummary> = MutableStateFlow(DailyUsageSummary())
    override suspend fun hasUsagePermission(): Boolean = true
    override suspend fun getTodayUsageMinutes(packageName: String): Long = usageMap[packageName] ?: 0L
}

class FakeSettingsRepo : SettingsRepository {
    val dumbModeFlow = MutableStateFlow(false)
    override val isOnboardingCompleted: Flow<Boolean> = MutableStateFlow(true)
    override val isAdaptiveFrictionEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val defaultFocusDurationMinutes: Flow<Int> = MutableStateFlow(25)
    override val isLocalAnalyticsEnabled: Flow<Boolean> = MutableStateFlow(true)
    override val appTheme: Flow<AppTheme> = MutableStateFlow(AppTheme.PURE_BLACK)
    override val isDumbModeEnabled: Flow<Boolean> = dumbModeFlow
    override val isAutoGrayscaleInFocusEnabled: Flow<Boolean> = MutableStateFlow(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) {}
    override suspend fun setAdaptiveFrictionEnabled(enabled: Boolean) {}
    override suspend fun setDefaultFocusDurationMinutes(minutes: Int) {}
    override suspend fun setLocalAnalyticsEnabled(enabled: Boolean) {}
    override suspend fun setAppTheme(theme: AppTheme) {}
    override suspend fun setDumbModeEnabled(enabled: Boolean) {
        dumbModeFlow.value = enabled
    }
    override suspend fun setAutoGrayscaleInFocusEnabled(enabled: Boolean) {}
}

class RuleEngineTest {

    private lateinit var timeLimitRepo: FakeRuleTimeLimitRepo
    private lateinit var usageStatsRepo: FakeRuleUsageStatsRepo
    private lateinit var settingsRepo: FakeSettingsRepo
    private lateinit var ruleEngine: RuleEngine

    @Before
    fun setup() {
        timeLimitRepo = FakeRuleTimeLimitRepo()
        usageStatsRepo = FakeRuleUsageStatsRepo()
        settingsRepo = FakeSettingsRepo()
        ruleEngine = RuleEngine(settingsRepo, timeLimitRepo, usageStatsRepo)
    }

    @Test
    fun `step 1 - essential apps are always allowed immediately`() = runTest {
        val app = InstalledApp(
            packageName = "com.phone.app",
            activityName = "MainActivity",
            label = "Phone",
            isEssential = true
        )
        val context = RuleContext(app = app, activeSession = null)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.EmergencyAllow)
        assertEquals("com.phone.app", (decision as AppLaunchDecision.EmergencyAllow).packageName)
    }

    @Test
    fun `step 2 - dumb mode blocks non-core applications`() = runTest {
        settingsRepo.setDumbModeEnabled(true)
        val nonCoreApp = InstalledApp(
            packageName = "com.social.app",
            activityName = "SocialActivity",
            label = "Social Feed",
            isEssential = false
        )
        val context = RuleContext(app = nonCoreApp, activeSession = null)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.Block)
        assertEquals("Dumb Phone Detox Mode is active. Only essential communication apps are allowed.", (decision as AppLaunchDecision.Block).reason)
    }

    @Test
    fun `step 2 - dumb mode allows core phone and sms applications`() = runTest {
        settingsRepo.setDumbModeEnabled(true)
        val dialerApp = InstalledApp(
            packageName = "com.android.dialer",
            activityName = "DialerActivity",
            label = "Phone",
            isEssential = false
        )
        val context = RuleContext(app = dialerApp, activeSession = null)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.Allow)
    }

    @Test
    fun `step 3 - strict focus session blocks non-essential applications`() = runTest {
        val session = FocusSession(
            id = "sess-1",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Study"),
            mode = FocusMode.STRICT,
            status = SessionStatus.ACTIVE
        )
        val app = InstalledApp(
            packageName = "com.distracting.game",
            activityName = "GameActivity",
            label = "Game",
            isEssential = false
        )
        val context = RuleContext(app = app, activeSession = session)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.Block)
    }

    @Test
    fun `step 3 - light focus session returns show friction`() = runTest {
        val session = FocusSession(
            id = "sess-light",
            startTime = System.currentTimeMillis() - 1000,
            endTime = System.currentTimeMillis() + 600000,
            durationMinutes = 10,
            goal = FocusGoal(id = "g-1", title = "Reading"),
            mode = FocusMode.LIGHT,
            status = SessionStatus.ACTIVE
        )
        val app = InstalledApp(
            packageName = "com.distracting.game",
            activityName = "GameActivity",
            label = "Game",
            isEssential = false
        )
        val context = RuleContext(app = app, activeSession = session)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.ShowFriction)
    }

    @Test
    fun `step 4 - daily time limit exhaustion blocks app launch`() = runTest {
        timeLimitRepo.setLimit("com.youtube", limitMinutes = 30, isEnabled = true)
        usageStatsRepo.usageMap["com.youtube"] = 35L // exceeded!

        val app = InstalledApp(
            packageName = "com.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            isEssential = false
        )
        val context = RuleContext(app = app, activeSession = null)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.Block)
        assertTrue((decision as AppLaunchDecision.Block).reason.contains("Daily limit of 30m reached"))
    }

    @Test
    fun `step 5 - default allows launch when no restrictions match`() = runTest {
        timeLimitRepo.setLimit("com.notes", limitMinutes = 60, isEnabled = true)
        usageStatsRepo.usageMap["com.notes"] = 10L // well within limit

        val app = InstalledApp(
            packageName = "com.notes",
            activityName = "NotesActivity",
            label = "Notes",
            isEssential = false
        )
        val context = RuleContext(app = app, activeSession = null)
        val decision = ruleEngine.evaluate(context)

        assertTrue(decision is AppLaunchDecision.Allow)
    }
}
