package com.minimalphone.core.model

enum class AppTheme(val displayName: String, val description: String) {
    PURE_BLACK(
        displayName = "Pure OLED Black",
        description = "Deep black (#000000) with crisp white typography for maximum battery conservation."
    ),
    E_INK_PAPER(
        displayName = "E-Ink Paper Monochrome",
        description = "Warm paper white background with charcoal text for high-contrast day reading."
    ),
    WARM_AMBER(
        displayName = "Warm Amber Minimal",
        description = "Muted warm amber typography on deep espresso background for night focus."
    );

    companion object {
        fun fromName(name: String?): AppTheme {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: PURE_BLACK
        }
    }
}
