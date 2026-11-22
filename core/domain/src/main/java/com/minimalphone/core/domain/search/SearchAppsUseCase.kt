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
import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.QuickContact
import com.minimalphone.core.model.SearchResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchAppsUseCase @Inject constructor(
    private val appRepository: AppRepository,
    private val contactRepository: ContactRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val settingsRepository: SettingsRepository,
    private val dailyAppUsageDao: DailyAppUsageDao,
    private val quarantineEngine: QuarantineEngine = QuarantineEngine(),
    private val searchRanker: SearchRanker = SearchRanker(),
    private val appRuleRepository: AppRuleRepository? = null
) {

    private val phoneRegex = Regex("^[+0-9()\\-\\s]{3,}$")

    /**
     * Searches apps and pinned quick contacts according to visibility and ranking rules.
     */
    fun search(query: String): Flow<List<SearchResult>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return flowOf(emptyList())
        }

        val sinceDate = getFourteenDaysAgoDate()

        val appsAndRulesFlow = combine(
            appRepository.getAllApps(),
            appRuleRepository?.observeAllRules() ?: flowOf(emptyList())
        ) { apps, rules -> apps to rules }

        return combine(
            appsAndRulesFlow,
            contactRepository.observePinnedContacts(),
            focusSessionRepository.getActiveSession(),
            settingsRepository.isDumbModeEnabled,
            settingsRepository.searchFuzzyEnabled
        ) { (allApps, allRules), pinnedContacts, activeSession, isDumbMode, fuzzyEnabled ->
            val now = System.currentTimeMillis()
            val customLabels = allRules.associate { it.packageName to it.customLabel }
            val launchCounts = runCatching {
                dailyAppUsageDao.getLaunchCountsSince(sinceDate).associate { it.packageName to it.totalLaunches }
            }.getOrDefault(emptyMap())

            val isStrictOrDeep = activeSession != null &&
                    activeSession.isCurrentlyActive &&
                    (activeSession.mode == FocusMode.STRICT || activeSession.mode == FocusMode.DEEP_FOCUS)

            val candidates = mutableListOf<RankCandidate>()

            // 1. Filter and build app candidates
            for (app in allApps) {
                // Rule: In STRICT and DEEP focus, Managed apps are hidden
                if (isStrictOrDeep && !app.isEssential && app.category != AppCategory.ESSENTIAL) {
                    continue
                }

                // Rule: In Dumb Mode, only home-pinned and Always Available apps are searchable
                if (isDumbMode && !app.isFavoriteOnHome && !app.isEssential && app.category != AppCategory.ESSENTIAL) {
                    continue
                }

                // Rule: Hidden apps are excluded
                if (app.category == AppCategory.HIDDEN || app.category == AppCategory.SYSTEM_INTERNAL) {
                    continue
                }

                val isQuarantined = quarantineEngine.isQuarantined(app.quarantineUntil, now)
                val badgeText = if (isQuarantined) {
                    val remaining = quarantineEngine.remainingHours(app.quarantineUntil, now)
                    "Available in $remaining"
                } else null

                val customLabel = customLabels[app.packageName]
                val rawLabel = customLabel ?: app.label
                val displayLabel = if (app.isClone) "$rawLabel (clone)" else rawLabel

                val labelsToMatch = mutableListOf<String>()
                labelsToMatch.add(rawLabel)
                if (rawLabel != app.label) {
                    labelsToMatch.add(app.label)
                }
                labelsToMatch.add(app.packageName)

                val result = SearchResult.App(
                    installedApp = app,
                    customLabel = customLabel,
                    isQuarantined = isQuarantined,
                    quarantineBadgeText = badgeText
                )

                candidates.add(
                    RankCandidate(
                        result = result,
                        labels = labelsToMatch,
                        displayLabel = displayLabel,
                        isPinned = app.isFavoriteOnHome,
                        recentLaunchCount = launchCounts[app.packageName] ?: 0
                    )
                )
            }

            // 2. Add Pinned Quick Contacts
            for (contact in pinnedContacts) {
                val result = SearchResult.QuickContact(contact)
                candidates.add(
                    RankCandidate(
                        result = result,
                        labels = listOf(contact.name, contact.phoneNumber),
                        displayLabel = contact.name,
                        isPinned = true,
                        recentLaunchCount = 0
                    )
                )
            }

            // 3. Rank candidates
            val ranked = searchRanker.rank(trimmed, candidates, fuzzyEnabled)

            // 4. Dial Number candidate if query looks like phone number
            val finalResults = ranked.toMutableList()
            if (phoneRegex.matches(trimmed) && trimmed.any { it.isDigit() }) {
                // Check if already an exact match or add DialNumber
                finalResults.add(SearchResult.DialNumber(trimmed))
            }

            // Limit to at most 6 results as required by 23.1
            finalResults.take(6)
        }
    }

    private fun getFourteenDaysAgoDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -14)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }
}
