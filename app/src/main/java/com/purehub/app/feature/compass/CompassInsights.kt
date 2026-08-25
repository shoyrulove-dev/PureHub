package com.purehub.app.feature.compass

import kotlin.math.abs

object CompassInsights {
    fun normalize(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    fun signedDeviation(current: Float, target: Float): Float {
        val delta = normalize(target) - normalize(current)
        return when {
            delta > 180f -> delta - 360f
            delta < -180f -> delta + 360f
            else -> delta
        }
    }

    fun guidance(current: Float, target: Float, tolerance: Float = 2f): String {
        val deviation = signedDeviation(current, target)
        if (abs(deviation) <= tolerance) return "Target aligned"
        return "Turn ${if (deviation > 0) "clockwise" else "counter-clockwise"} ${abs(deviation).toInt()}°"
    }
}
