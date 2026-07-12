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
}
