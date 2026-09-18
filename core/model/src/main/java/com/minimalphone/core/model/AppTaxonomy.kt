package com.minimalphone.core.model

/**
 * Detailed taxonomy categories for apps on the device (M21).
 */
enum class AppTaxonomy(val displayName: String) {
    COMMUNICATION("Calls & Messages"),
    PRODUCTIVITY("Productivity & Work"),
    READING_EDUCATION("Reading & Study"),
    SOCIAL_MEDIA("Social & Networking"),
    ENTERTAINMENT_GAMES("Games & Media"),
    UTILITY_TOOLS("Utilities & Tools"),
    FINANCE("Finance & Banking"),
    HEALTH("Health & Fitness"),
    OTHER("Other")
}
