package com.minimalphone.core.domain.quarantine

import com.minimalphone.core.model.QuarantineHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuarantineEngineTest {

    private lateinit var engine: QuarantineEngine
    private val now = 1_700_000_000_000L

    @Before
    fun setup() {
        engine = QuarantineEngine()
    }

    @Test
    fun `OFF quarantine returns null until time`() {
        assertNull(engine.quarantineUntil(now, QuarantineHours.OFF))
    }

    @Test
    fun `ONE hour quarantine returns correct until time`() {
        val until = engine.quarantineUntil(now, QuarantineHours.ONE)
        assertNotNull(until)
        assertEquals(now + 60 * 60 * 1000L, until)
    }

    @Test
    fun `TWENTY_FOUR hour quarantine returns correct until time`() {
        val until = engine.quarantineUntil(now, QuarantineHours.TWENTY_FOUR)
        assertNotNull(until)
        assertEquals(now + 24 * 60 * 60 * 1000L, until)
    }

    @Test
    fun `SEVENTY_TWO hour quarantine returns correct until time`() {
        val until = engine.quarantineUntil(now, QuarantineHours.SEVENTY_TWO)
        assertNotNull(until)
        assertEquals(now + 72 * 60 * 60 * 1000L, until)
    }

    @Test
    fun `app with future quarantine timestamp is quarantined`() {
        val until = now + 1000L
        assertTrue(engine.isQuarantined(until, now))
    }

    @Test
    fun `app with past quarantine timestamp is not quarantined`() {
        val until = now - 1000L
        assertFalse(engine.isQuarantined(until, now))
    }

    @Test
    fun `app with null quarantine timestamp is not quarantined`() {
        assertFalse(engine.isQuarantined(null, now))
    }

    @Test
    fun `remaining millis is zero for null quarantine`() {
        assertEquals(0L, engine.remainingMillis(null, now))
    }

    @Test
    fun `remaining millis is zero for expired quarantine`() {
        assertEquals(0L, engine.remainingMillis(now - 1000L, now))
    }

    @Test
    fun `remaining hours formats hours correctly`() {
        val twoHoursFromNow = now + 2 * 60 * 60 * 1000L + 30 * 60 * 1000L
        val result = engine.remainingHours(twoHoursFromNow, now)
        assertTrue(result.startsWith("2h"))
    }

    @Test
    fun `remaining hours formats minutes only correctly`() {
        val thirtyMinsFromNow = now + 30 * 60 * 1000L
        val result = engine.remainingHours(thirtyMinsFromNow, now)
        assertEquals("30m", result)
    }
}
