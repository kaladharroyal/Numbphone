package com.minimalphone.core.domain.taxonomy

import com.minimalphone.core.model.AppTaxonomy
import com.minimalphone.core.model.ClassificationSource
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

    // ─────────── M21: WebAPK Tests ───────────

    @Test
    fun `chromium webapk package is detected as webapk`() {
        assertTrue(classifier.isWebApk("org.chromium.webapk.a1b2c3d4e5f6"))
    }

    @Test
    fun `non-webapk package is not detected as webapk`() {
        assertFalse(classifier.isWebApk("com.instagram.android"))
    }

    @Test
    fun `package ending in webapk is detected`() {
        assertTrue(classifier.isWebApk("some.banking.webapk"))
    }

    // ─────────── M21: Clone Indicator Tests ───────────

    @Test
    fun `package with clone suffix is detected as clone`() {
        assertTrue(classifier.isCloneIndicator("com.instagram.android.clone"))
    }

    @Test
    fun `package with dual in path is detected as clone`() {
        assertTrue(classifier.isCloneIndicator("com.facebook.dual.katana"))
    }

    @Test
    fun `normal package is not detected as clone`() {
        assertFalse(classifier.isCloneIndicator("com.instagram.android"))
    }

    // ─────────── M21: Sibling / Family Taxonomy ───────────

    @Test
    fun `instagram lite classifies to same taxonomy as instagram`() {
        val taxonomy1 = classifier.classify("com.instagram.android", "Instagram")
        val taxonomy2 = classifier.classify("com.instagram.lite", "Instagram Lite")
        assertEquals(taxonomy1, taxonomy2)
    }

    // ─────────── M21: ClassificationSource Tests ───────────

    @Test
    fun `webapk package returns WEB_APP classification source`() {
        val source = classifier.classifySource("org.chromium.webapk.abc123")
        assertEquals(ClassificationSource.WEB_APP, source)
    }

    @Test
    fun `normal package returns AUTOMATIC_DEFAULT classification source`() {
        val source = classifier.classifySource("com.instagram.android")
        assertEquals(ClassificationSource.AUTOMATIC_DEFAULT, source)
    }
}
