package com.minimalphone.core.model

/**
 * Auto-launch policy when search returns a single match.
 */
enum class AutoLaunchMode {
    /** Never auto-launch; user must tap the result or press IME Go. */
    OFF,

    /**
     * Default mode: automatically launch if the single match is an Essential
     * or home-pinned app, after a 500 ms cancellable delay.
     */
    ESSENTIAL_AND_PINNED,

    /** Auto-launch any single match (including managed apps) after 500 ms. */
    ALL
}

/**
 * Distinct result types displayed in Home Search.
 */
sealed interface SearchResult {
    val id: String
    val displayLabel: String

    data class App(
        val installedApp: InstalledApp,
        val customLabel: String? = null,
        val isQuarantined: Boolean = false,
        val quarantineBadgeText: String? = null
    ) : SearchResult {
        override val id: String get() = "app:${installedApp.packageName}"
        override val displayLabel: String get() = customLabel ?: installedApp.label
    }

    data class QuickContact(
        val contact: com.minimalphone.core.model.QuickContact
    ) : SearchResult {
        override val id: String get() = "contact:${contact.id}"
        override val displayLabel: String get() = contact.name
    }

    data class DialNumber(
        val number: String
    ) : SearchResult {
        override val id: String get() = "dial:$number"
        override val displayLabel: String get() = "Dial $number"
    }
}
