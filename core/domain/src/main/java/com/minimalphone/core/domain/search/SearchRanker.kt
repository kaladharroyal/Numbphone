package com.minimalphone.core.domain.search

import com.minimalphone.core.model.InstalledApp
import com.minimalphone.core.model.SearchResult
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

data class RankedItem<T>(
    val item: T,
    val score: Int,
    val displayLabel: String
)

@Singleton
class SearchRanker @Inject constructor() {

    companion object {
        const val SCORE_LABEL_PREFIX = 1000
        const val SCORE_WORD_PREFIX = 800
        const val SCORE_SUBSTRING = 600
        const val SCORE_SUBSEQUENCE = 400
        const val SCORE_TYPO_TOLERANCE = 200
        const val BOOST_PINNED = 50
        const val MAX_LAUNCH_BOOST = 50
    }

    fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Calculates the match score for a normalized query against candidate labels.
     * Returns 0 if there is no match.
     */
    fun matchScore(
        normalizedQuery: String,
        candidateLabels: List<String>,
        fuzzyEnabled: Boolean = true
    ): Int {
        if (normalizedQuery.isBlank()) return 0

        var highestScore = 0
        for (label in candidateLabels) {
            val normalizedLabel = normalize(label)
            if (normalizedLabel.isBlank()) continue

            val score = scoreCandidate(normalizedQuery, normalizedLabel, fuzzyEnabled)
            if (score > highestScore) {
                highestScore = score
            }
        }
        return highestScore
    }

    private fun scoreCandidate(
        query: String,
        label: String,
        fuzzyEnabled: Boolean
    ): Int {
        // 1. Exact or Label Prefix
        if (label.startsWith(query)) {
            // Bonus for exact match or closer length
            val exactBonus = if (label == query) 50 else 0
            return SCORE_LABEL_PREFIX + exactBonus
        }

        // 2. Word Prefix
        val words = label.split(" ")
        for (word in words) {
            if (word.startsWith(query)) {
                return SCORE_WORD_PREFIX
            }
        }

        // 3. Substring
        if (label.contains(query)) {
            return SCORE_SUBSTRING
        }

        // 4. In-order subsequence
        if (isSubsequence(query, label)) {
            return SCORE_SUBSEQUENCE
        }

        // 5. Typo tolerance (edit distance <= 1 for query length >= 4)
        if (fuzzyEnabled && query.length >= 4) {
            // Check full label
            if (levenshteinDistance(query, label) <= 1) {
                return SCORE_TYPO_TOLERANCE
            }
            // Check individual words
            for (word in words) {
                if (levenshteinDistance(query, word) <= 1) {
                    return SCORE_TYPO_TOLERANCE
                }
            }
        }

        return 0
    }

    fun isSubsequence(query: String, target: String): Boolean {
        var queryIdx = 0
        var targetIdx = 0
        while (queryIdx < query.length && targetIdx < target.length) {
            if (query[queryIdx] == target[targetIdx]) {
                queryIdx++
            }
            targetIdx++
        }
        return queryIdx == query.length
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,       // deletion
                    dp[i][j - 1] + 1,       // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Ranks a collection of search results given a raw query string.
     */
    fun rank(
        rawQuery: String,
        items: List<RankCandidate>,
        fuzzyEnabled: Boolean = true
    ): List<SearchResult> {
        val normalizedQuery = normalize(rawQuery)
        if (normalizedQuery.isBlank()) return emptyList()

        val scored = mutableListOf<RankedItem<SearchResult>>()

        for (candidate in items) {
            val baseScore = matchScore(normalizedQuery, candidate.labels, fuzzyEnabled)
            if (baseScore > 0) {
                var totalScore = baseScore
                if (candidate.isPinned) {
                    totalScore += BOOST_PINNED
                }
                totalScore += min(candidate.recentLaunchCount, MAX_LAUNCH_BOOST)

                scored.add(RankedItem(candidate.result, totalScore, candidate.displayLabel))
            }
        }

        // Sort descending by score, stable tie-break: alphabetical by displayLabel
        scored.sortWith(
            compareByDescending<RankedItem<SearchResult>> { it.score }
                .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
        )

        return scored.map { it.item }
    }
}

data class RankCandidate(
    val result: SearchResult,
    val labels: List<String>,
    val displayLabel: String,
    val isPinned: Boolean = false,
    val recentLaunchCount: Int = 0
)
