package com.minimalphone.core.domain.insights

import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.PauseRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.BehavioralInsights
import com.minimalphone.core.model.PauseOutcome
import com.minimalphone.core.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetBehavioralInsightsUseCase @Inject constructor(
    private val blockedAttemptRepository: BlockedAttemptRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val pauseRepository: PauseRepository? = null
) {
    operator fun invoke(): Flow<BehavioralInsights> {
        val todayStartMillis = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        return combine(
            blockedAttemptRepository.getBlockedAttempts(),
            focusSessionRepository.getAllSessions(),
            usageStatsRepository.getDailyUsageSummary()
        ) { blockedAttempts, sessions, usageSummary ->
            val todayBlocks = blockedAttempts.filter { it.timestamp >= todayStartMillis }
            val todaySessions = sessions.filter {
                it.startTime >= todayStartMillis && it.status == SessionStatus.COMPLETED
            }

            val topBlocked = todayBlocks
                .groupBy { it.appLabel }
                .mapValues { it.value.size }
                .toList()
                .sortedByDescending { it.second }
                .take(5)

            val totalFocusMins = todaySessions.sumOf { it.durationMinutes }
            val totalReflections = todayBlocks.count { !it.userIntentReason.isNullOrBlank() }

            val todayPauseEvents = pauseRepository?.getEventsSince(todayStartMillis) ?: emptyList()
            val totalPauses = todayPauseEvents.size
            val walkedAway = todayPauseEvents.count { it.outcome == PauseOutcome.LEFT }
            val walkAwayRate = if (totalPauses > 0) ((walkedAway * 100) / totalPauses) else 0

            BehavioralInsights(
                totalBlockedAttemptsToday = todayBlocks.size,
                topBlockedApps = topBlocked,
                totalFocusMinutesToday = totalFocusMins,
                focusSessionCountToday = todaySessions.size,
                totalReflectionsCountToday = totalReflections,
                dailyProgressVsAverage = if (todayBlocks.size > 0) 15 else 0,
                totalPausesShownToday = totalPauses,
                pausesWalkedAwayToday = walkedAway,
                walkAwayRatePercentToday = walkAwayRate
            )
        }
    }
}
