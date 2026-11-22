package com.minimalphone.feature.homescreen

import com.minimalphone.core.model.GestureType
import kotlin.math.abs
import kotlin.math.hypot

data class TouchPoint(
    val x: Float,
    val y: Float,
    val timeMillis: Long
)

data class ScreenBounds(
    val width: Float,
    val height: Float,
    val leftInset: Float = 0f,
    val rightInset: Float = 0f,
    val topInset: Float = 0f,
    val bottomInset: Float = 0f
)

/**
 * Recognizer for home screen swipe gestures with distance, velocity, dominance,
 * and system gesture inset edge-exclusion rules (M23.4).
 */
class GestureRecognizer(
    val minDistancePx: Float = 96f,
    val minVelocityPxPerSec: Float = 400f,
    val dominanceRatio: Float = 2.0f
) {

    /**
     * Classifies a swipe gesture. Returns null if distance, velocity, axis dominance,
     * or system gesture edge exclusion checks fail.
     */
    fun classifySwipe(
        start: TouchPoint,
        end: TouchPoint,
        bounds: ScreenBounds
    ): GestureType? {
        // 1. Conflict avoidance: Ignore gestures that start inside system gesture insets
        if (start.x < bounds.leftInset ||
            start.x > (bounds.width - bounds.rightInset) ||
            start.y < bounds.topInset ||
            start.y > (bounds.height - bounds.bottomInset)
        ) {
            return null
        }

        val dx = end.x - start.x
        val dy = end.y - start.y
        val absDx = abs(dx)
        val absDy = abs(dy)
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        // 2. Minimum distance threshold
        if (distance < minDistancePx) {
            return null
        }

        // 3. Minimum velocity threshold
        val dtMillis = (end.timeMillis - start.timeMillis).coerceAtLeast(1)
        val velocity = (distance / dtMillis) * 1000f
        if (velocity < minVelocityPxPerSec) {
            return null
        }

        // 4. Dominant axis check (dominant axis must be at least 2x the other)
        return if (absDy >= absDx * dominanceRatio) {
            if (dy < 0) GestureType.SWIPE_UP else GestureType.SWIPE_DOWN
        } else if (absDx >= absDy * dominanceRatio) {
            if (dx < 0) GestureType.SWIPE_LEFT else GestureType.SWIPE_RIGHT
        } else {
            null // Ambiguous diagonal swipe
        }
    }
}
