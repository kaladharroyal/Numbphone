package com.minimalphone.core.data.scanner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppScannerTest {

    @Test
    fun `isEssentialPackage correctly identifies essential communication apps`() {
        assertTrue(AppScanner.isEssentialPackage("com.google.android.dialer", "Phone"))
        assertTrue(AppScanner.isEssentialPackage("com.google.android.apps.messaging", "Messages"))
        assertTrue(AppScanner.isEssentialPackage("com.google.android.GoogleCamera", "Camera"))
        assertTrue(AppScanner.isEssentialPackage("com.google.android.calendar", "Calendar"))
        assertTrue(AppScanner.isEssentialPackage("com.google.android.deskclock", "Clock"))
        assertTrue(AppScanner.isEssentialPackage("com.google.android.contacts", "Contacts"))
    }

    @Test
    fun `isEssentialPackage correctly excludes distracting entertainment apps`() {
        assertFalse(AppScanner.isEssentialPackage("com.google.android.youtube", "YouTube"))
        assertFalse(AppScanner.isEssentialPackage("com.instagram.android", "Instagram"))
        assertFalse(AppScanner.isEssentialPackage("com.zhiliaoapp.musically", "TikTok"))
        assertFalse(AppScanner.isEssentialPackage("com.twitter.android", "X"))
        assertFalse(AppScanner.isEssentialPackage("com.reddit.frontpage", "Reddit"))
        assertFalse(AppScanner.isEssentialPackage("com.netflix.mediaclient", "Netflix"))
    }
}
