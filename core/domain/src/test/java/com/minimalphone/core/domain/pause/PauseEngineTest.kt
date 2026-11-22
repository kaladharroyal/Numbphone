package com.minimalphone.core.domain.pause

import com.minimalphone.core.data.db.dao.DailyAppUsageDao
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.PauseRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.family.AppFamilyResolver
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppFamily
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseEvent
import com.minimalphone.core.model.PauseEventKind
import com.minimalphone.core.model.PauseOutcome
import com.minimalphone.core.model.PauseStyle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class PauseEngineTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var pauseRepository: PauseRepository
    private lateinit var appRepository: AppRepository
    private lateinit var emergencyAccessManager: EmergencyAccessManager
    private lateinit var pauseGrantStore: PauseGrantStore
    private lateinit var appFamilyResolver: AppFamilyResolver
    private lateinit var dailyAppUsageDao: DailyAppUsageDao
    private lateinit var pauseEngine: PauseEngine

    private var currentTime = 1_000_000L

    @Before
    fun setup() {
        currentTime = 1_000_000L
        settingsRepository = mockk(relaxed = true)
        pauseRepository = mockk(relaxed = true)
        appRepository = mockk(relaxed = true)
        emergencyAccessManager = mockk(relaxed = true)
        pauseGrantStore = PauseGrantStore(customClock = { currentTime })
        appFamilyResolver = mockk(relaxed = true)
        dailyAppUsageDao = mockk(relaxed = true)

        coEvery { settingsRepository.pauseGlobalEnabled } returns MutableStateFlow(true)
        coEvery { settingsRepository.defaultPauseSeconds } returns MutableStateFlow(5)
        coEvery { settingsRepository.defaultSessionMinutes } returns MutableStateFlow(10)
        coEvery { settingsRepository.defaultPauseStyle } returns MutableStateFlow(PauseStyle.BREATHE)
        coEvery { settingsRepository.progressiveDelayEnabled } returns MutableStateFlow(false)

        coEvery { emergencyAccessManager.isEmergencyOrEssential(any()) } returns false
        coEvery { pauseRepository.getAppPauseConfigSync(any()) } returns null

        coEvery { appFamilyResolver.getFamilyMembers("com.instagram.android") } returns listOf(
            "com.instagram.android", "com.instagram.lite"
        )

        pauseEngine = PauseEngine(
            settingsRepository = settingsRepository,
            pauseRepository = pauseRepository,
            appRepository = appRepository,
            emergencyAccessManager = emergencyAccessManager,
            pauseGrantStore = pauseGrantStore,
            appFamilyResolver = appFamilyResolver,
            dailyAppUsageDao = dailyAppUsageDao
        )
    }

    @Test
    fun `essential or emergency apps never pause`() = runBlocking {
        coEvery { emergencyAccessManager.isEmergencyOrEssential("com.google.android.dialer") } returns true

        val decision = pauseEngine.evaluate("com.google.android.dialer")
        assertTrue(decision is PauseDecision.NoPause)
    }

    @Test
    fun `managed app without grant returns Pause with configured delay`() = runBlocking {
        val managedApp = InstalledApp(
            packageName = "com.instagram.android",
            activityName = "MainActivity",
            label = "Instagram",
            category = AppCategory.MANAGED,
            isEssential = false
        )
        coEvery { appRepository.getApp("com.instagram.android") } returns managedApp

        val decision = pauseEngine.evaluate("com.instagram.android")
        assertTrue(decision is PauseDecision.Pause)
        val pause = decision as PauseDecision.Pause
        assertEquals(5, pause.delaySeconds)
        assertEquals(PauseStyle.BREATHE, pause.config.style)
    }

    @Test
    fun `managed app with valid grant returns NoPause`() = runBlocking {
        val managedApp = InstalledApp(
            packageName = "com.instagram.android",
            activityName = "MainActivity",
            label = "Instagram",
            category = AppCategory.MANAGED,
            isEssential = false
        )
        coEvery { appRepository.getApp("com.instagram.android") } returns managedApp

        // Grant session
        pauseEngine.grantSession("com.instagram.android", sessionMinutes = 10)

        val decision = pauseEngine.evaluate("com.instagram.android")
        assertTrue(decision is PauseDecision.NoPause)
    }

    @Test
    fun `app with pause disabled in per-app config returns NoPause`() = runBlocking {
        coEvery { pauseRepository.getAppPauseConfigSync("com.instagram.android") } returns PauseConfig(
            packageName = "com.instagram.android",
            enabled = false
        )

        val decision = pauseEngine.evaluate("com.instagram.android")
        assertTrue(decision is PauseDecision.NoPause)
    }

    @Test
    fun `global pause disabled returns NoPause`() = runBlocking {
        coEvery { settingsRepository.pauseGlobalEnabled } returns MutableStateFlow(false)

        val decision = pauseEngine.evaluate("com.instagram.android")
        assertTrue(decision is PauseDecision.NoPause)
    }

    @Test
    fun `progressive delay escalates delay based on daily launch count capped at 30s`() = runBlocking {
        coEvery { settingsRepository.progressiveDelayEnabled } returns MutableStateFlow(true)
        val todayStr = LocalDate.now().toString()

        // 4 earlier launches today -> 5s base + 4 * 2s = 13s
        coEvery { dailyAppUsageDao.getLaunchCount("com.instagram.android", todayStr) } returns 4

        val decision = pauseEngine.evaluate("com.instagram.android")
        assertTrue(decision is PauseDecision.Pause)
        assertEquals(13, (decision as PauseDecision.Pause).delaySeconds)

        // 20 earlier launches today -> 5s base + 40s = 45s -> capped at 30s
        coEvery { dailyAppUsageDao.getLaunchCount("com.instagram.android", todayStr) } returns 20
        val cappedDecision = pauseEngine.evaluate("com.instagram.android")
        assertEquals(30, (cappedDecision as PauseDecision.Pause).delaySeconds)
    }

    @Test
    fun `granting session shares grant with family members`() = runBlocking {
        pauseEngine.grantSession("com.instagram.android", sessionMinutes = 10)

        // Sibling in family is now also granted
        assertTrue(pauseGrantStore.hasValidGrant("com.instagram.lite"))
    }

    @Test
    fun `recordOutcome records event to pause repository`() = runBlocking {
        pauseEngine.recordOutcome(
            packageName = "com.instagram.android",
            kind = PauseEventKind.LAUNCH,
            outcome = PauseOutcome.LEFT,
            secondsShown = 5
        )

        coVerify {
            pauseRepository.recordPauseEvent(
                match {
                    it.packageName == "com.instagram.android" &&
                    it.kind == PauseEventKind.LAUNCH &&
                    it.outcome == PauseOutcome.LEFT &&
                    it.secondsShown == 5
                }
            )
        }
    }
}
