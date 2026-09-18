package com.minimalphone.core.domain.taxonomy

import com.minimalphone.core.model.AppTaxonomy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppTaxonomyClassifierTest {

    private lateinit var classifier: AppTaxonomyClassifier

    @Before
    fun setup() {
        classifier = AppTaxonomyClassifier()
    }

    @Test
    fun `classify correctly maps communication apps`() {
        assertEquals(AppTaxonomy.COMMUNICATION, classifier.classify("com.google.android.dialer", "Phone"))
        assertEquals(AppTaxonomy.COMMUNICATION, classifier.classify("org.telegram.messenger", "Telegram"))
        assertEquals(AppTaxonomy.COMMUNICATION, classifier.classify("com.whatsapp", "WhatsApp"))
    }

    @Test
    fun `classify correctly maps social media and entertainment`() {
        assertEquals(AppTaxonomy.SOCIAL_MEDIA, classifier.classify("com.instagram.android", "Instagram"))
        assertEquals(AppTaxonomy.SOCIAL_MEDIA, classifier.classify("com.twitter.android", "X"))
        assertEquals(AppTaxonomy.ENTERTAINMENT_GAMES, classifier.classify("com.google.android.youtube", "YouTube"))
        assertEquals(AppTaxonomy.ENTERTAINMENT_GAMES, classifier.classify("com.spotify.music", "Spotify"))
    }

    @Test
    fun `classify correctly maps productivity and education`() {
        assertEquals(AppTaxonomy.PRODUCTIVITY, classifier.classify("md.obsidian", "Obsidian"))
        assertEquals(AppTaxonomy.PRODUCTIVITY, classifier.classify("notion.id", "Notion"))
        assertEquals(AppTaxonomy.READING_EDUCATION, classifier.classify("com.amazon.kindle", "Kindle"))
        assertEquals(AppTaxonomy.READING_EDUCATION, classifier.classify("com.duolingo", "Duolingo"))
    }

    @Test
    fun `isEssentialByDefault correctly flags critical system utilities`() {
        assertTrue(classifier.isEssentialByDefault("com.google.android.dialer", "Phone"))
        assertTrue(classifier.isEssentialByDefault("com.google.android.apps.messaging", "Messages"))
        assertTrue(classifier.isEssentialByDefault("com.android.settings", "Settings"))
        assertFalse(classifier.isEssentialByDefault("com.instagram.android", "Instagram"))
    }
}
