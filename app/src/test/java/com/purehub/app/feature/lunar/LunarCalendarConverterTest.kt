package com.purehub.app.feature.lunar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
