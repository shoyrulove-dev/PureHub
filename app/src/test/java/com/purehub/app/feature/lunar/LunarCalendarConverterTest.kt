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

    @Test
    fun solarTermsAreDetectedFromLongitudeCrossing() {
        val frostDescent = LunarCalendarConverter.details(LocalDate.of(2026, 10, 23))
        assertEquals("Sương giáng", frostDescent.solarTerm)
    }

    @Test
    fun auspiciousHoursUseTheDayBranchTable() {
        val details = LunarCalendarConverter.details(LocalDate.of(2026, 10, 12))
        assertEquals(6, details.auspiciousHours.size)
        assertEquals(details.auspiciousHours.size, details.auspiciousHours.distinct().size)
    }

    @Test
    fun supportedRangeRoundTripsForBothRegionalEngines() {
        LunarRegion.entries.forEach { region ->
            for (year in 1900..2100 step 10) {
                listOf(1, 4, 7, 10).forEach { month ->
                    val solar = LocalDate.of(year, month, 15)
                    val lunar = LunarCalendarConverter.convertSolarToLunar(solar, region)
                    assertEquals("$region $solar", solar, LunarCalendarConverter.convertLunarToSolar(lunar, region))
                }
            }
        }
    }

    @Test
    fun regionalTimezoneCanProduceDifferentLunarDates() {
        val hasDifference = (0L..3650L).any { offset ->
            val date = LocalDate.of(2020, 1, 1).plusDays(offset)
            LunarCalendarConverter.convertSolarToLunar(date, LunarRegion.Vietnam) !=
                LunarCalendarConverter.convertSolarToLunar(date, LunarRegion.China)
        }
        assert(hasDifference)
    }
}
