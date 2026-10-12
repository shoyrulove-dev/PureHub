package com.purehub.app.feature.lunar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class LunarCalendarConverterTest {
    @Test
    fun tet2026ConvertsToFirstLunarDay() {
        val lunar = LunarCalendarConverter.convertSolarToLunar(LocalDate.of(2026, 2, 17))
        assertEquals(1, lunar.day)
        assertEquals(1, lunar.month)
        assertFalse(lunar.isLeapMonth)
    }

    @Test
    fun descriptionIncludesCanChi() {
        val description = LunarCalendarConverter.describeDate(LocalDate.of(2026, 9, 25))
        assert(description.canchiLabel.contains("Year"))
    }

    @Test
    fun lunarNewYearRoundTripsAtVietnamTimezone() {
        val solar = LocalDate.of(2026, 2, 17)
        val lunar = LunarCalendarConverter.convertSolarToLunar(solar)
        assertEquals(solar, LunarCalendarConverter.convertLunarToSolar(lunar))
    }

    @Test
    fun detailsExposeTraditionalAndMoonInformation() {
        val details = LunarCalendarConverter.details(LocalDate.of(2026, 2, 17))
        assertNotNull(details.traditionalMarker)
        assert(details.auspiciousHours.isNotEmpty())
        assert(details.moonIlluminationPercent in 0..100)
    }
}
