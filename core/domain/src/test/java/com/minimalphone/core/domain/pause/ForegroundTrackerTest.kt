package com.minimalphone.core.domain.pause

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForegroundTrackerTest {

    private lateinit var tracker: ForegroundTracker

    @Before
    fun setup() {
        tracker = ForegroundTracker()
    }

    @Test
    fun `ignores blank packages and own launcher package`() {
        assertTrue(tracker.shouldIgnore("", ownPackageName = "com.minimalphone"))
        assertTrue(tracker.shouldIgnore("com.minimalphone", ownPackageName = "com.minimalphone"))
    }

    @Test
    fun `ignores systemui and default keyboards`() {
        assertTrue(tracker.shouldIgnore("com.android.systemui"))
        assertTrue(tracker.shouldIgnore("com.google.android.inputmethod.latin"))
        val customImes = setOf("com.custom.keyboard")
        assertTrue(tracker.shouldIgnore("com.custom.keyboard", enabledImePackages = customImes))
    }

    @Test
    fun `emits transition when new valid application comes to foreground`() {
        val transition = tracker.onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "MainActivity",
            nowMs = 1000L
        )

        assertNotNull(transition)
        assertEquals(null, transition?.previousPackage)
        assertEquals("com.instagram.android", transition?.currentPackage)
        assertEquals("com.instagram.android", tracker.getCurrentForegroundPackage())
    }

    @Test
    fun `debounces rapid window events for the exact same package within 300ms`() {
        tracker.onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "MainActivity",
            nowMs = 1000L
        )

        val rapidSubsequentEvent = tracker.onWindowStateChanged(
            packageName = "com.instagram.android",
            className = "SubActivity",
            nowMs = 1150L // 150ms later < 300ms
        )

        assertNull(rapidSubsequentEvent)
    }

    @Test
    fun `emits transition when switching from one app to another`() {
        tracker.onWindowStateChanged(
            packageName = "com.instagram.android",
            nowMs = 1000L
        )

        val switchTransition = tracker.onWindowStateChanged(
            packageName = "com.twitter.android",
            nowMs = 1500L
        )

        assertNotNull(switchTransition)
        assertEquals("com.instagram.android", switchTransition?.previousPackage)
        assertEquals("com.twitter.android", switchTransition?.currentPackage)
    }
}
