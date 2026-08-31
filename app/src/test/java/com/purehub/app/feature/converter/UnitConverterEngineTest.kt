package com.purehub.app.feature.converter

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitConverterEngineTest {
    @Test
    fun convertsMetersToFeet() {
        val result = UnitConverterEngine.convert(
            value = "1",
            category = ConverterCategory.LENGTH,
            fromIndex = 0,
            toIndex = 2,
        )

        assertEquals("3.2808", result)
    }

    @Test
    fun convertsCelsiusToFahrenheit() {
        val result = UnitConverterEngine.convert(
            value = "100",
            category = ConverterCategory.TEMPERATURE,
            fromIndex = 0,
            toIndex = 1,
        )

        assertEquals("212", result)
    }

    @Test
    fun convertsCommonTravelAndEverydayUnits() {
        assertEquals("62.1371", UnitConverterEngine.convert("100", ConverterCategory.LENGTH, 1, 6))
        assertEquals("96.5606", UnitConverterEngine.convert("60", ConverterCategory.SPEED, 2, 1))
        assertEquals("14.5038", UnitConverterEngine.convert("1", ConverterCategory.PRESSURE, 2, 3))
        assertEquals("3.7854", UnitConverterEngine.convert("1", ConverterCategory.VOLUME, 3, 0))
    }

    @Test
    fun rejectsOutOfRangeUnitIndexes() {
        assertEquals("", UnitConverterEngine.convert("1", ConverterCategory.LENGTH, -1, 0))
        assertEquals("", UnitConverterEngine.convert("1", ConverterCategory.LENGTH, 0, 99))
    }
}
