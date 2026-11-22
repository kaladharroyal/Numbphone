package com.minimalphone.core.domain.quarantine

import com.minimalphone.core.model.QuarantineHours
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuarantineEngine @Inject constructor() {

    fun quarantineUntil(now: Long, hours: QuarantineHours): Long? {
        val h = hours.hours ?: return null
        return now + h * 60L * 60L * 1000L
    }

    fun isQuarantined(quarantineUntil: Long?, now: Long): Boolean {
        return quarantineUntil != null && quarantineUntil > now
    }

    fun remainingMillis(quarantineUntil: Long?, now: Long): Long {
        return if (quarantineUntil == null) 0L else maxOf(0L, quarantineUntil - now)
    }

    fun remainingHours(quarantineUntil: Long?, now: Long): String {
        val millis = remainingMillis(quarantineUntil, now)
        val hours = millis / (60 * 60 * 1000L)
        val minutes = (millis % (60 * 60 * 1000L)) / (60 * 1000L)
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }
}
