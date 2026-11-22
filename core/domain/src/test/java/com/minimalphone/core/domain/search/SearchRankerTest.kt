package com.minimalphone.core.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SearchRankerTest {

    private lateinit var ranker: SearchRanker

    @Before
    fun setup() {
        ranker = SearchRanker()
    }

    @Test
    fun `normalize strips diacritics, lowercase and extra whitespace`() {
        assertEquals("cafe", ranker.normalize("Café"))
        assertEquals("uber eats", ranker.normalize("  Über   Eats  "))
        assertEquals("numbphone", ranker.normalize("NumbPhone"))
    }

    @Test
    fun `label prefix gets highest tier score`() {
        val score = ranker.matchScore("cam", listOf("Camera"))
        assertTrue("Prefix score should be >= SCORE_LABEL_PREFIX", score >= SearchRanker.SCORE_LABEL_PREFIX)
    }

    @Test
    fun `exact match gets prefix score plus exact bonus`() {
        val prefixScore = ranker.matchScore("cam", listOf("Camera"))
        val exactScore = ranker.matchScore("camera", listOf("Camera"))
        assertTrue(exactScore > prefixScore)
        assertEquals(SearchRanker.SCORE_LABEL_PREFIX + 50, exactScore)
    }

    @Test
    fun `word prefix gets word prefix tier score`() {
        val score = ranker.matchScore("sett", listOf("System Settings"))
        assertEquals(SearchRanker.SCORE_WORD_PREFIX, score)
    }

    @Test
    fun `substring gets substring tier score`() {
        val score = ranker.matchScore("tube", listOf("YouTube"))
        assertEquals(SearchRanker.SCORE_SUBSTRING, score)
    }

    @Test
    fun `in-order subsequence gets subsequence tier score`() {
        val score = ranker.matchScore("yt", listOf("YouTube"))
        assertEquals(SearchRanker.SCORE_SUBSEQUENCE, score)
    }

    @Test
    fun `typo tolerance allows edit distance 1 for query length 4 or more when fuzzy enabled`() {
        val fuzzyScore = ranker.matchScore("camerx", listOf("Camera"), fuzzyEnabled = true)
        assertEquals(SearchRanker.SCORE_TYPO_TOLERANCE, fuzzyScore)

        val nonFuzzyScore = ranker.matchScore("camerx", listOf("Camera"), fuzzyEnabled = false)
        assertEquals(0, nonFuzzyScore)
    }

    @Test
    fun `typo tolerance is disabled for query length less than 4`() {
        val score = ranker.matchScore("cmx", listOf("Camera"), fuzzyEnabled = true)
        assertEquals(0, score)
    }

    @Test
    fun `rank sorts by score, pinned boost, launch boost, and alphabetical tie break`() {
        val candidates = listOf(
            RankCandidate(
                result = com.minimalphone.core.model.SearchResult.DialNumber("123"),
                labels = listOf("Beta App"),
                displayLabel = "Beta App",
                isPinned = false,
                recentLaunchCount = 0
            ),
            RankCandidate(
                result = com.minimalphone.core.model.SearchResult.DialNumber("456"),
                labels = listOf("Alpha App"),
                displayLabel = "Alpha App",
                isPinned = false,
                recentLaunchCount = 0
            ),
            RankCandidate(
                result = com.minimalphone.core.model.SearchResult.DialNumber("789"),
                labels = listOf("Pinned App"),
                displayLabel = "Pinned App",
                isPinned = true,
                recentLaunchCount = 0
            )
        )

        // All match substring "App" with SCORE_SUBSTRING
        val ranked = ranker.rank("App", candidates, fuzzyEnabled = true)

        // Pinned App should be first due to BOOST_PINNED (+50)
        // Alpha App should be second due to alphabetical tie break
        // Beta App should be third
        assertEquals(3, ranked.size)
        assertEquals("Pinned App", (ranked[0] as? com.minimalphone.core.model.SearchResult.DialNumber)?.let { "Pinned App" })
        assertEquals(candidates[2].result, ranked[0])
        assertEquals(candidates[1].result, ranked[1]) // Alpha App
        assertEquals(candidates[0].result, ranked[2]) // Beta App
    }
}
