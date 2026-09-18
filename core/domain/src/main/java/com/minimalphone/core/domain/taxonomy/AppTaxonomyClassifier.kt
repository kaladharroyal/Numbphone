package com.minimalphone.core.domain.taxonomy

import com.minimalphone.core.model.AppCategory
import com.minimalphone.core.model.AppTaxonomy
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
            lowerPkg.contains("dialer") || lowerPkg.contains("telecom") || lowerPkg.contains("phone") ||
                    lowerPkg.contains("messaging") || lowerPkg.contains("mms") || lowerPkg.contains("whatsapp") ||
                    lowerPkg.contains("telegram") || lowerPkg.contains("signal") ||
                    lowerLabel.contains("phone") || lowerLabel.contains("messages") -> AppTaxonomy.COMMUNICATION

            lowerPkg.contains("instagram") || lowerPkg.contains("twitter") || lowerPkg.contains("tiktok") ||
                    lowerPkg.contains("facebook") || lowerPkg.contains("reddit") || lowerPkg.contains("snapchat") ||
                    lowerPkg.contains("threads") -> AppTaxonomy.SOCIAL_MEDIA

            lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("spotify") ||
                    lowerPkg.contains("games") || lowerPkg.contains("twitch") || lowerPkg.contains("primevideo") -> AppTaxonomy.ENTERTAINMENT_GAMES

            lowerPkg.contains("docs") || lowerPkg.contains("sheets") || lowerPkg.contains("slides") ||
                    lowerPkg.contains("notion") || lowerPkg.contains("obsidian") || lowerPkg.contains("calendar") ||
                    lowerPkg.contains("notes") || lowerPkg.contains("todo") || lowerPkg.contains("trello") -> AppTaxonomy.PRODUCTIVITY

            lowerPkg.contains("kindle") || lowerPkg.contains("books") || lowerPkg.contains("medium") ||
                    lowerPkg.contains("duolingo") || lowerPkg.contains("anki") || lowerPkg.contains("coursera") -> AppTaxonomy.READING_EDUCATION

            lowerPkg.contains("bank") || lowerPkg.contains("pay") || lowerPkg.contains("wallet") ||
                    lowerPkg.contains("finance") || lowerPkg.contains("crypto") -> AppTaxonomy.FINANCE

            lowerPkg.contains("fit") || lowerPkg.contains("health") || lowerPkg.contains("strava") ||
                    lowerPkg.contains("workout") -> AppTaxonomy.HEALTH

            lowerPkg.contains("settings") || lowerPkg.contains("clock") || lowerPkg.contains("camera") ||
                    lowerPkg.contains("calculator") || lowerPkg.contains("files") -> AppTaxonomy.UTILITY_TOOLS

            else -> AppTaxonomy.OTHER
        }
    }

    fun isEssentialByDefault(packageName: String, label: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()
        return lowerLabel == "phone" || lowerLabel == "messages" || lowerLabel == "settings" ||
                lowerLabel == "camera" || lowerLabel == "clock" || lowerLabel == "dialer" ||
                lowerPkg.contains("dialer") || lowerPkg.contains("telecom")
    }
}
