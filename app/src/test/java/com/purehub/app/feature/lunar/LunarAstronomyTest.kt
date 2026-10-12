package com.purehub.app.feature.lunar

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class LunarAstronomyTest {
    @Test
    fun hoChiMinhEstimateStaysWithinPhysicalRanges() {
        val result = LunarAstronomy.calculate(
            date = LocalDate.of(2026, 10, 12),
            latitude = 10.8231,
            longitude = 106.6297,
            zone = ZoneId.of("Asia/Ho_Chi_Minh"),
        )

        assertTrue(result.illuminationPercent in 0..100)
        assertTrue(result.moonAgeDays >= 0.0 && result.moonAgeDays < 29.530588853)
        assertTrue(result.moonrise != null || result.moonset != null)
        assertTrue(result.altitudeDegrees in -90.0..90.0)
        assertTrue(result.azimuthDegrees in 0.0..360.0)
        assertTrue(result.nextNewMoon.toLocalDate() >= LocalDate.of(2026, 10, 12))
        assertTrue(result.nextFullMoon.toLocalDate() >= LocalDate.of(2026, 10, 12))
    }
}
