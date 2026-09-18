package com.minimalphone.core.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Named focus preset (e.g. "Deep Work (50m)", "Quick Study (25m)", "Night Wind-Down").
 */
data class FocusPreset(
    val id: String,
    val title: String,
    val durationMinutes: Int,
    val mode: FocusMode = FocusMode.STRICT,
    val category: String = "WORK",
    val isDefault: Boolean = false
)

/**
 * Automated scheduled focus recurring rule.
 */
data class FocusSchedule(
    val id: String,
    val presetId: String? = null,
    val title: String,
    val daysOfWeek: Set<DayOfWeek>,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val mode: FocusMode = FocusMode.STRICT,
    val isEnabled: Boolean = true
)
