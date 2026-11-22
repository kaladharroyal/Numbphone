package com.minimalphone.core.domain.pause

import com.minimalphone.core.data.db.dao.DailyAppUsageDao
import com.minimalphone.core.data.repository.AppRepository
import com.minimalphone.core.data.repository.PauseRepository
import com.minimalphone.core.data.repository.SettingsRepository
import com.minimalphone.core.domain.emergency.EmergencyAccessManager
import com.minimalphone.core.domain.family.AppFamilyResolver
import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.BypassRoute
import com.minimalphone.core.model.PauseConfig
import com.minimalphone.core.model.PauseEvent
import com.minimalphone.core.model.PauseEventKind
import com.minimalphone.core.model.PauseOutcome
import com.minimalphone.core.model.PauseStyle
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PauseDecision {
    data object NoPause : PauseDecision
    data class Pause(val config: PauseConfig, val delaySeconds: Int) : PauseDecision
    data class Reintervene(val config: PauseConfig, val elapsedMinutes: Int, val delaySeconds: Int) : PauseDecision
}

@Singleton
class PauseEngine @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val pauseRepository: PauseRepository,
    private val appRepository: AppRepository,
    private val emergencyAccessManager: EmergencyAccessManager,
    private val pauseGrantStore: PauseGrantStore,
    private val appFamilyResolver: AppFamilyResolver,
    private val dailyAppUsageDao: DailyAppUsageDao
) {

    suspend fun evaluate(
        packageName: String,
        source: BypassRoute = BypassRoute.LAUNCHER,
        now: Long = System.currentTimeMillis()
    ): PauseDecision {
        // 1. Global pause switch
        val globalEnabled = runCatching { settingsRepository.pauseGlobalEnabled.first() }.getOrDefault(true)
        if (!globalEnabled) return PauseDecision.NoPause

        // 2. Essential and emergency apps never pause
        if (emergencyAccessManager.isEmergencyOrEssential(packageName)) {
            return PauseDecision.NoPause
        }
        val app = appRepository.getApp(packageName)
        if (app != null && (app.isEssential || app.category == AppCategory.ESSENTIAL)) {
            return PauseDecision.NoPause
        }

        // 3. Check for existing valid grant
        if (pauseGrantStore.hasValidGrant(packageName)) {
            return PauseDecision.NoPause
        }

        // 4. Resolve app-level pause config
        val storedConfig = pauseRepository.getAppPauseConfigSync(packageName)
        if (storedConfig != null && !storedConfig.enabled) {
            return PauseDecision.NoPause
        }

        val config = storedConfig ?: run {
            val defSec = runCatching { settingsRepository.defaultPauseSeconds.first() }.getOrDefault(5)
            val defMins = runCatching { settingsRepository.defaultSessionMinutes.first() }.getOrDefault(10)
            val defStyle = runCatching { settingsRepository.defaultPauseStyle.first() }.getOrDefault(PauseStyle.BREATHE)
            PauseConfig(
                packageName = packageName,
                enabled = true,
                pauseSeconds = defSec,
                style = defStyle,
                sessionMinutes = defMins,
                reinterventionEnabled = true
            )
        }

        // 5. Progressive delay calculation (optional)
        val isProgressive = runCatching { settingsRepository.progressiveDelayEnabled.first() }.getOrDefault(false)
        val delaySeconds = if (isProgressive) {
            val todayStr = LocalDate.now().toString()
            val earlierLaunches = dailyAppUsageDao.getLaunchCount(packageName, todayStr) ?: 0
            (config.pauseSeconds + (earlierLaunches * 2)).coerceIn(config.pauseSeconds, 30)
        } else {
            config.pauseSeconds
        }

        return PauseDecision.Pause(config = config, delaySeconds = delaySeconds)
    }

    /**
     * Grants a mindful session to the package and its family members.
     */
    fun grantSession(packageName: String, sessionMinutes: Int) {
        val familyMembers = appFamilyResolver.getFamilyMembers(packageName)
        pauseGrantStore.grant(packageName, sessionMinutes, familyMembers)
    }

    /**
     * Records an outcome (PROCEEDED or LEFT) in the pause events ledger.
     */
    suspend fun recordOutcome(
        packageName: String,
        kind: PauseEventKind = PauseEventKind.LAUNCH,
        outcome: PauseOutcome,
        reflectionReason: String? = null,
        secondsShown: Int = 0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        pauseRepository.recordPauseEvent(
            PauseEvent(
                packageName = packageName,
                timestamp = timestamp,
                kind = kind,
                outcome = outcome,
                reflectionReason = reflectionReason,
                secondsShown = secondsShown
            )
        )
    }
}
