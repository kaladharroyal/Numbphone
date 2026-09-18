package com.minimalphone.core.model

/**
 * Behavioral insights aggregating distractions, intentions, and focus achievements.
 */
data class BehavioralInsights(
    val totalBlockedAttemptsToday: Int = 0,
    val topBlockedApps: List<Pair<String, Int>> = emptyList(),
    val totalFocusMinutesToday: Int = 0,
    val focusSessionCountToday: Int = 0,
    val totalReflectionsCountToday: Int = 0,
    val dailyProgressVsAverage: Int = 0 // percent change vs 7-day average
)

/**
 * 7-day Weekly Wellbeing Report with Digital Balance Score.
 */
data class WeeklyWellbeingReport(
    val startDate: String = "",
    val endDate: String = "",
    val totalScreenTimeHours: Float = 0f,
    val dailyAverageScreenTimeHours: Float = 0f,
    val totalFocusHours: Float = 0f,
    val totalDistractionsBlocked: Int = 0,
    val digitalBalanceScore: Int = 85, // 0 - 100 score
    val keyTakeaway: String = "Great balance this week!"
)
