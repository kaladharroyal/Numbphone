package com.minimalphone.core.domain.search

import com.minimalphone.core.data.db.dao.DailyAppUsageDao
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.AppRuleRepository
import com.minimalphone.core.data.repository.ContactRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.domain.quarantine.QuarantineEngine
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.FocusMode
import com.minimalphone.core.model.FocusSession
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.model.SearchResult
import com.minimalphone.core.model.SessionStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchVisibilityTest {

    private lateinit var appRepository: AppRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var focusSessionRepository: FocusSessionRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var dailyAppUsageDao: DailyAppUsageDao
    private lateinit var appRuleRepository: AppRuleRepository
    private lateinit var quarantineEngine: QuarantineEngine
    private lateinit var searchAppsUseCase: SearchAppsUseCase

    private val appsFlow = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val contactsFlow = MutableStateFlow<List<QuickContact>>(emptyList())
    private val sessionFlow = MutableStateFlow<FocusSession?>(null)
    private val isDumbModeFlow = MutableStateFlow(false)
    private val fuzzyEnabledFlow = MutableStateFlow(true)
    private val rulesFlow = MutableStateFlow<List<com.minimalphone.core.model.AppRule>>(emptyList())

    @Before
    fun setup() {
        appRepository = mockk(relaxed = true)
        contactRepository = mockk(relaxed = true)
        focusSessionRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        dailyAppUsageDao = mockk(relaxed = true)
        appRuleRepository = mockk(relaxed = true)
        quarantineEngine = QuarantineEngine()

        every { appRepository.getAllApps() } returns appsFlow
        every { contactRepository.observePinnedContacts() } returns contactsFlow
        every { focusSessionRepository.getActiveSession() } returns sessionFlow
        every { settingsRepository.isDumbModeEnabled } returns isDumbModeFlow
        every { settingsRepository.searchFuzzyEnabled } returns fuzzyEnabledFlow
        every { appRuleRepository.observeAllRules() } returns rulesFlow
        coEvery { dailyAppUsageDao.getLaunchCountsSince(any()) } returns emptyList()

        searchAppsUseCase = SearchAppsUseCase(
            appRepository = appRepository,
            contactRepository = contactRepository,
            focusSessionRepository = focusSessionRepository,
            settingsRepository = settingsRepository,
            dailyAppUsageDao = dailyAppUsageDao,
            quarantineEngine = quarantineEngine,
            searchRanker = SearchRanker(),
            appRuleRepository = appRuleRepository
        )
    }

    @Test
    fun `managed apps are hidden during STRICT or DEEP focus while essential apps remain`() = runBlocking {
        val phoneApp = InstalledApp(
            packageName = "com.android.dialer",
            activityName = "DialerActivity",
            label = "Phone",
            category = AppCategory.ESSENTIAL,
            isEssential = true
        )
        val youtubeApp = InstalledApp(
            packageName = "com.google.android.youtube",
            activityName = "YouTubeActivity",
            label = "YouTube",
            category = AppCategory.MANAGED,
            isEssential = false
        )
        appsFlow.value = listOf(phoneApp, youtubeApp)

        // Active DEEP session
        val now = System.currentTimeMillis()
        sessionFlow.value = FocusSession(
            id = "deep-1",
            startTime = now - 1000,
            endTime = now + 1800_000,
            durationMinutes = 30,
            goal = com.minimalphone.core.model.FocusGoal(id = "g1", title = "Testing"),
            mode = FocusMode.DEEP_FOCUS,
            status = SessionStatus.ACTIVE
        )

        val results = searchAppsUseCase.search("").first()
        assertTrue(results.isEmpty())

        // Search for YouTube -> hidden
        val ytResults = searchAppsUseCase.search("You").first()
        assertTrue(ytResults.isEmpty())

        // Search for Phone -> visible
        val phoneResults = searchAppsUseCase.search("Pho").first()
        assertEquals(1, phoneResults.size)
        assertTrue(phoneResults[0] is SearchResult.App)
        assertEquals("com.android.dialer", (phoneResults[0] as SearchResult.App).installedApp.packageName)
    }

    @Test
    fun `in Dumb Mode only pinned and essential apps are searchable`() = runBlocking {
        isDumbModeFlow.value = true

        val essentialApp = InstalledApp(
            packageName = "com.android.dialer",
            activityName = "DialerActivity",
            label = "Phone",
            category = AppCategory.ESSENTIAL,
            isEssential = true
        )
        val pinnedManagedApp = InstalledApp(
            packageName = "com.notes.app",
            activityName = "NotesActivity",
            label = "Notes",
            category = AppCategory.MANAGED,
            isFavoriteOnHome = true
        )
        val unpinnedManagedApp = InstalledApp(
            packageName = "com.browser.app",
            activityName = "BrowserActivity",
            label = "Browser",
            category = AppCategory.MANAGED,
            isFavoriteOnHome = false
        )
        appsFlow.value = listOf(essentialApp, pinnedManagedApp, unpinnedManagedApp)

        val browserResults = searchAppsUseCase.search("Browser").first()
        assertTrue(browserResults.isEmpty())

        val notesResults = searchAppsUseCase.search("Notes").first()
        assertEquals(1, notesResults.size)

        val phoneResults = searchAppsUseCase.search("Phone").first()
        assertEquals(1, phoneResults.size)
    }

    @Test
    fun `quarantined apps appear with quarantine badge`() = runBlocking {
        val now = System.currentTimeMillis()
        val futureQuarantine = now + (3 * 3600 * 1000L) // 3 hours in future
        val quarantinedApp = InstalledApp(
            packageName = "com.game.fun",
            activityName = "GameActivity",
            label = "Fun Game",
            category = AppCategory.MANAGED,
            quarantineUntil = futureQuarantine
        )
        appsFlow.value = listOf(quarantinedApp)

        val results = searchAppsUseCase.search("Game").first()
        assertEquals(1, results.size)
        val appResult = results[0] as SearchResult.App
        assertTrue(appResult.isQuarantined)
        assertNotNull(appResult.quarantineBadgeText)
        assertTrue(appResult.quarantineBadgeText!!.contains("Available in"))
    }

    @Test
    fun `cloned apps appear with clone suffix`() = runBlocking {
        val cloneApp = InstalledApp(
            packageName = "com.instagram.android",
            activityName = "MainActivity",
            label = "Instagram",
            category = AppCategory.MANAGED,
            isClone = true
        )
        appsFlow.value = listOf(cloneApp)

        val results = searchAppsUseCase.search("Instagram").first()
        assertEquals(1, results.size)
        val appResult = results[0] as SearchResult.App
        assertEquals(true, appResult.installedApp.isClone)
    }

    @Test
    fun `phone query adds dial number result`() = runBlocking {
        appsFlow.value = emptyList()

        val results = searchAppsUseCase.search("+1-555-0199").first()
        assertEquals(1, results.size)
        assertTrue(results[0] is SearchResult.DialNumber)
        assertEquals("+1-555-0199", (results[0] as SearchResult.DialNumber).number)
    }
}
