package com.minimalphone.feature.homescreen

import com.minimalphone.core.model.GestureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GestureRecognizerTest {

    private lateinit var recognizer: GestureRecognizer
    private val bounds = ScreenBounds(
        width = 1080f,
        height = 2400f,
        leftInset = 40f,
        rightInset = 40f,
        topInset = 60f,
        bottomInset = 60f
    )

    @Before
    fun setup() {
        recognizer = GestureRecognizer(
            minDistancePx = 96f,
            minVelocityPxPerSec = 400f,
            dominanceRatio = 2.0f
        )
    }

    @Test
    fun `swipe under minimum distance is rejected`() {
        val start = TouchPoint(500f, 1000f, 0L)
        val end = TouchPoint(500f, 950f, 50L) // 50px distance < 96px
        val result = recognizer.classifySwipe(start, end, bounds)
        assertNull(result)
    }

    @Test
    fun `slow drag under velocity threshold is rejected`() {
        val start = TouchPoint(500f, 1000f, 0L)
        val end = TouchPoint(500f, 850f, 1000L) // 150px in 1000ms = 150 px/s < 400 px/s
        val result = recognizer.classifySwipe(start, end, bounds)
        assertNull(result)
    }

    @Test
    fun `ambiguous diagonal swipe without 2x axis dominance is rejected`() {
        val start = TouchPoint(500f, 1000f, 0L)
        val end = TouchPoint(650f, 850f, 100L) // dx=150, dy=-150, ratio=1.0 < 2.0
        val result = recognizer.classifySwipe(start, end, bounds)
        assertNull(result)
    }

    @Test
    fun `gesture starting inside system gesture insets is rejected`() {
        // Starts in left inset (< 40)
        val leftStart = TouchPoint(20f, 1000f, 0L)
        val leftEnd = TouchPoint(300f, 1000f, 100L)
        assertNull(recognizer.classifySwipe(leftStart, leftEnd, bounds))

        // Starts in right inset (> 1040)
        val rightStart = TouchPoint(1060f, 1000f, 0L)
        val rightEnd = TouchPoint(800f, 1000f, 100L)
        assertNull(recognizer.classifySwipe(rightStart, rightEnd, bounds))

        // Starts in top inset (< 60)
        val topStart = TouchPoint(500f, 30f, 0L)
        val topEnd = TouchPoint(500f, 300f, 100L)
        assertNull(recognizer.classifySwipe(topStart, topEnd, bounds))

        // Starts in bottom inset (> 2340)
        val bottomStart = TouchPoint(500f, 2370f, 0L)
        val bottomEnd = TouchPoint(500f, 2100f, 100L)
        assertNull(recognizer.classifySwipe(bottomStart, bottomEnd, bounds))
    }

    @Test
    fun `valid vertical and horizontal swipes are recognized correctly`() {
        // Swipe Up (dy < 0)
        val upResult = recognizer.classifySwipe(
            TouchPoint(500f, 1000f, 0L),
            TouchPoint(500f, 750f, 100L), // dy = -250px, 2500 px/s
            bounds
        )
        assertEquals(GestureType.SWIPE_UP, upResult)

        // Swipe Down (dy > 0)
        val downResult = recognizer.classifySwipe(
            TouchPoint(500f, 750f, 0L),
            TouchPoint(500f, 1000f, 100L), // dy = 250px, 2500 px/s
            bounds
        )
        assertEquals(GestureType.SWIPE_DOWN, downResult)

        // Swipe Left (dx < 0)
        val leftResult = recognizer.classifySwipe(
            TouchPoint(800f, 1000f, 0L),
            TouchPoint(500f, 1000f, 100L), // dx = -300px, 3000 px/s
            bounds
        )
        assertEquals(GestureType.SWIPE_LEFT, leftResult)

        // Swipe Right (dx > 0)
        val rightResult = recognizer.classifySwipe(
            TouchPoint(500f, 1000f, 0L),
            TouchPoint(800f, 1000f, 100L), // dx = 300px, 3000 px/s
            bounds
        )
        assertEquals(GestureType.SWIPE_RIGHT, rightResult)
    }
}
