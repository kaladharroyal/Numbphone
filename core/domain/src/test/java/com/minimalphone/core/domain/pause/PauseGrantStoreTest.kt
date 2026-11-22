package com.minimalphone.core.domain.pause

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PauseGrantStoreTest {

    private var currentTime = 1_000_000L
    private lateinit var store: PauseGrantStore

    @Before
    fun setup() {
        currentTime = 1_000_000L
        store = PauseGrantStore(customClock = { currentTime })
    }

    @Test
    fun `grant is valid immediately after granting`() {
        store.grant("com.instagram.android", sessionMinutes = 10)
        assertTrue(store.hasValidGrant("com.instagram.android"))
        assertEquals(600_000L, store.remainingMillis("com.instagram.android"))
    }

    @Test
    fun `grant expires after session minutes elapse`() {
        store.grant("com.instagram.android", sessionMinutes = 5)
        assertTrue(store.hasValidGrant("com.instagram.android"))

        // Advance 5 minutes + 1ms
        currentTime += (5 * 60 * 1000L) + 1L
        assertFalse(store.hasValidGrant("com.instagram.android"))
        assertEquals(0L, store.remainingMillis("com.instagram.android"))
    }

    @Test
    fun `grant survives short away switch within 30 second exit grace`() {
        store.grant("com.instagram.android", sessionMinutes = 10)
        store.onAppBackground("com.instagram.android")

        // User switches away for 20 seconds
        currentTime += 20_000L
        store.onAppForeground("com.instagram.android")

        assertTrue(store.hasValidGrant("com.instagram.android"))
    }

    @Test
    fun `grant is revoked if away longer than 30 second exit grace`() {
        store.grant("com.instagram.android", sessionMinutes = 10)
        store.onAppBackground("com.instagram.android")

        // User switches away for 35 seconds (exceeds 30s)
        currentTime += 35_000L

        assertFalse(store.hasValidGrant("com.instagram.android"))
    }

    @Test
    fun `grant is shared across all family members when familyPackages is provided`() {
        val family = listOf("com.instagram.android", "com.instagram.lite", "com.instagram.barcelona")
        store.grant("com.instagram.android", sessionMinutes = 10, familyPackages = family)

        assertTrue(store.hasValidGrant("com.instagram.android"))
        assertTrue(store.hasValidGrant("com.instagram.lite"))
        assertTrue(store.hasValidGrant("com.instagram.barcelona"))
    }

    @Test
    fun `revokeGrant removes specific package`() {
        store.grant("com.instagram.android", sessionMinutes = 10)
        store.revokeGrant("com.instagram.android")
        assertFalse(store.hasValidGrant("com.instagram.android"))
    }

    @Test
    fun `clear removes all grants`() {
        store.grant("com.instagram.android", sessionMinutes = 10)
        store.grant("com.facebook.katana", sessionMinutes = 10)
        store.clear()
        assertFalse(store.hasValidGrant("com.instagram.android"))
        assertFalse(store.hasValidGrant("com.facebook.katana"))
    }
}
