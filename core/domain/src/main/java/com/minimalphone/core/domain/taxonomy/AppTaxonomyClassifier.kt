package com.minimalphone.core.domain.taxonomy

import com.minimalphone.core.model.AppTaxonomy
import com.minimalphone.core.model.ClassificationSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatically classifies installed apps into domain taxonomies and focus safety categories.
 */
@Singleton
class AppTaxonomyClassifier @Inject constructor() {

    fun classify(packageName: String, label: String): AppTaxonomy {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()

        return when {
            listOf("dialer", "telecom", "phone", "messaging", "mms", "whatsapp", "telegram", "signal").any(lowerPkg::contains) ||
                    lowerLabel.contains("phone") || lowerLabel.contains("messages") -> AppTaxonomy.COMMUNICATION

            listOf("instagram", "twitter", "tiktok", "facebook", "reddit", "snapchat", "threads").any(lowerPkg::contains) -> AppTaxonomy.SOCIAL_MEDIA

            listOf("youtube", "netflix", "spotify", "games", "twitch", "primevideo").any(lowerPkg::contains) -> AppTaxonomy.ENTERTAINMENT_GAMES

            listOf("docs", "sheets", "slides", "notion", "obsidian", "calendar", "notes", "todo", "trello").any(lowerPkg::contains) -> AppTaxonomy.PRODUCTIVITY

            listOf("kindle", "books", "medium", "duolingo", "anki", "coursera").any(lowerPkg::contains) -> AppTaxonomy.READING_EDUCATION

            listOf("bank", "pay", "wallet", "finance", "crypto").any(lowerPkg::contains) -> AppTaxonomy.FINANCE

            listOf("fit", "health", "strava", "workout").any(lowerPkg::contains) -> AppTaxonomy.HEALTH

            listOf("settings", "clock", "camera", "calculator", "files").any(lowerPkg::contains) -> AppTaxonomy.UTILITY_TOOLS

            else -> AppTaxonomy.OTHER
        }
    }

    fun isEssentialByDefault(packageName: String, label: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()
        val essentialLabels = setOf("phone", "messages", "settings", "camera", "clock", "dialer")
        return lowerLabel in essentialLabels || lowerPkg.contains("dialer") || lowerPkg.contains("telecom")
    }

    fun isWebApk(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.startsWith("org.chromium.webapk.") ||
               lower.startsWith("org.chromium.") ||
               lower.endsWith(".webapk")
    }

    fun isCloneIndicator(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return lower.contains(".clone.") || lower.contains(".work.") ||
               lower.contains(".dual.") || lower.contains(".second.") ||
               lower.contains(".parallel.") ||
               lower.endsWith(".clone") || lower.endsWith(".work")
    }

    fun classifySource(packageName: String): ClassificationSource {
        return if (isWebApk(packageName)) {
            ClassificationSource.WEB_APP
        } else {
            ClassificationSource.AUTOMATIC_DEFAULT
        }
    }
}
