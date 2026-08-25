package com.purehub.app.feature.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompassInsightsTest {
    @Test
    fun deviationUsesShortestTurnAcrossNorth() {
        assertEquals(20f, CompassInsights.signedDeviation(350f, 10f))
        assertEquals(-20f, CompassInsights.signedDeviation(10f, 350f))
    }

    @Test
    fun guidanceRecognizesAlignment() {
        assertEquals("Target aligned", CompassInsights.guidance(89f, 90f))
        assertTrue(CompassInsights.guidance(0f, 90f).contains("clockwise"))
    }
}
