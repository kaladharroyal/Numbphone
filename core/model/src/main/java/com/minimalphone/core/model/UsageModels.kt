package com.minimalphone.core.model

data class AppUsageStat(
    val packageName: String,
    val label: String,
    val totalTimeForegroundMillis: Long,
    val lastTimeUsed: Long = 0L,
    val category: AppCategory = AppCategory.MANAGED,
    val isEssential: Boolean = false
) {
    val totalMinutes: Long
        get() = totalTimeForegroundMillis / (1000 * 60)

    val formattedDuration: String
        get() {
            val totalSeconds = totalTimeForegroundMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                totalSeconds > 0 -> "${totalSeconds}s"
                else -> "0m"
            }
        }
}

data class DailyUsageSummary(
    val dateTimestamp: Long = System.currentTimeMillis(),
    val totalScreenTimeMillis: Long = 0L,
    val managedTimeMillis: Long = 0L,
    val essentialTimeMillis: Long = 0L,
    val appStats: List<AppUsageStat> = emptyList(),
    val hasUsagePermission: Boolean = false
) {
    val formattedTotalTime: String
        get() {
            val totalSeconds = totalScreenTimeMillis / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m"
                else -> "0m"
            }
        }

    val managedPercentage: Float
        get() = if (totalScreenTimeMillis > 0) (managedTimeMillis.toFloat() / totalScreenTimeMillis) else 0f
}
