package com.minimalphone.core.domain.insights

import com.minimalphone.core.data.repository.BlockedAttemptRepository
import com.minimalphone.core.data.repository.FocusSessionRepository
import com.minimalphone.core.data.repository.UsageStatsRepository
import com.minimalphone.core.model.SessionStatus
import com.minimalphone.core.model.WeeklyWellbeingReport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class GetWeeklyWellbeingReportUseCase @Inject constructor(
    private val blockedAttemptRepository: BlockedAttemptRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val usageStatsRepository: UsageStatsRepository
) {
    operator fun invoke(): Flow<WeeklyWellbeingReport> {
        val today = LocalDate.now()
        val weekAgo = today.minusDays(7)
        val weekAgoMillis = weekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")

        return combine(
            blockedAttemptRepository.getBlockedAttempts(),
            focusSessionRepository.getAllSessions(),
            usageStatsRepository.getDailyUsageSummary()
        ) { blockedAttempts, sessions, dailyUsage ->
            val weeklyBlocks = blockedAttempts.filter { it.timestamp >= weekAgoMillis }
            val weeklySessions = sessions.filter {
                it.startTime >= weekAgoMillis && it.status == SessionStatus.COMPLETED
            }

            val totalFocusHours = (weeklySessions.sumOf { it.durationMinutes } / 60.0f)
            val todayScreenHours = (dailyUsage.totalScreenTimeMillis / 3600000.0f)
            val estimatedWeeklyScreenHours = (todayScreenHours * 7f).coerceAtLeast(todayScreenHours)
            val dailyAvg = (estimatedWeeklyScreenHours / 7.0f)

            // Digital balance score: higher focus & lower screen time yields higher score (0-100)
            val balanceScore = (80f + (totalFocusHours * 3f) - (dailyAvg * 2f))
                .toInt()
                .coerceIn(40, 100)

            val takeaway = when {
                balanceScore >= 85 -> "Outstanding focus! You reclaimed significant time from distractions this week."
                balanceScore >= 70 -> "Solid progress. Your focus sessions are effectively blocking impulse checks."
                else -> "Try scheduling short 25m focus blocks during your high-distraction hours."
            }

            WeeklyWellbeingReport(
                startDate = weekAgo.format(dateFormatter),
                endDate = today.format(dateFormatter),
                totalScreenTimeHours = String.format("%.1f", estimatedWeeklyScreenHours).toFloatOrNull() ?: 0f,
                dailyAverageScreenTimeHours = String.format("%.1f", dailyAvg).toFloatOrNull() ?: 0f,
                totalFocusHours = String.format("%.1f", totalFocusHours).toFloatOrNull() ?: 0f,
                totalDistractionsBlocked = weeklyBlocks.size,
                digitalBalanceScore = balanceScore,
                keyTakeaway = takeaway
            )
        }
    }
}
