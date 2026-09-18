package com.minimalphone.core.model

/**
 * Domain model for an app's daily usage budget.
 * When [dailyLimitMinutes] > 0 and [enabled] is true,
 * the RuleEngine will block the app once the budget is exhausted today.
 */
data class AppBudget(
    val packageName: String,
    val appLabel: String = "",
    val dailyLimitMinutes: Int,
    val enabled: Boolean = true
)

/**
 * Domain model for an app's tracked usage on a specific calendar day.
 */
data class DailyAppUsage(
    val packageName: String,
    val appLabel: String = "",
    /** ISO-8601 date e.g. "2026-09-18" */
    val date: String,
    val foregroundMinutes: Int,
    val launchCount: Int,
    val blockedAttempts: Int
)
