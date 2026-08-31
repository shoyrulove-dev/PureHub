package com.purehub.app.feature.converter

import java.math.BigDecimal
import java.math.RoundingMode

enum class ConverterCategory(val title: String) {
    LENGTH("Length"),
    AREA("Area"),
    VOLUME("Volume"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    SPEED("Speed"),
    TIME("Time"),
    PRESSURE("Pressure"),
    ENERGY("Energy"),
    POWER("Power"),
    ANGLE("Angle"),
    STORAGE("Storage"),
}

data class ConverterUnit(
    val label: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double,
)

object UnitConverterEngine {
    val categories: Map<ConverterCategory, List<ConverterUnit>> = mapOf(
        ConverterCategory.LENGTH to listOf(
            ConverterUnit("Meters", { it }, { it }),
            ConverterUnit("Kilometers", { it * 1_000.0 }, { it / 1_000.0 }),
            ConverterUnit("Feet", { it * 0.3048 }, { it / 0.3048 }),
            ConverterUnit("Inches", { it * 0.0254 }, { it / 0.0254 }),
            ConverterUnit("Centimeters", { it / 100.0 }, { it * 100.0 }),
            ConverterUnit("Millimeters", { it / 1_000.0 }, { it * 1_000.0 }),
            ConverterUnit("Miles", { it * 1_609.344 }, { it / 1_609.344 }),
        ),
        ConverterCategory.AREA to listOf(
            ConverterUnit("Square meters", { it }, { it }),
            ConverterUnit("Square kilometers", { it * 1_000_000.0 }, { it / 1_000_000.0 }),
            ConverterUnit("Square feet", { it * 0.09290304 }, { it / 0.09290304 }),
            ConverterUnit("Square inches", { it * 0.00064516 }, { it / 0.00064516 }),
            ConverterUnit("Hectares", { it * 10_000.0 }, { it / 10_000.0 }),
            ConverterUnit("Acres", { it * 4_046.8564224 }, { it / 4_046.8564224 }),
        ),
        ConverterCategory.VOLUME to listOf(
            ConverterUnit("Liters", { it }, { it }),
            ConverterUnit("Milliliters", { it / 1_000.0 }, { it * 1_000.0 }),
            ConverterUnit("Cubic meters", { it * 1_000.0 }, { it / 1_000.0 }),
            ConverterUnit("US gallons", { it * 3.785411784 }, { it / 3.785411784 }),
            ConverterUnit("US cups", { it * 0.2365882365 }, { it / 0.2365882365 }),
            ConverterUnit("Tablespoons", { it * 0.0147867648 }, { it / 0.0147867648 }),
        ),
        ConverterCategory.WEIGHT to listOf(
            ConverterUnit("Kilograms", { it }, { it }),
            ConverterUnit("Grams", { it / 1_000.0 }, { it * 1_000.0 }),
            ConverterUnit("Pounds", { it * 0.45359237 }, { it / 0.45359237 }),
            ConverterUnit("Ounces", { it * 0.0283495231 }, { it / 0.0283495231 }),
        ),
        ConverterCategory.TEMPERATURE to listOf(
            ConverterUnit("Celsius", { it }, { it }),
            ConverterUnit("Fahrenheit", { (it - 32.0) * 5.0 / 9.0 }, { (it * 9.0 / 5.0) + 32.0 }),
            ConverterUnit("Kelvin", { it - 273.15 }, { it + 273.15 }),
        ),
        ConverterCategory.SPEED to listOf(
            ConverterUnit("Meters/second", { it }, { it }),
            ConverterUnit("Kilometers/hour", { it / 3.6 }, { it * 3.6 }),
            ConverterUnit("Miles/hour", { it * 0.44704 }, { it / 0.44704 }),
            ConverterUnit("Knots", { it * 0.514444 }, { it / 0.514444 }),
        ),
        ConverterCategory.TIME to listOf(
            ConverterUnit("Seconds", { it }, { it }),
            ConverterUnit("Minutes", { it * 60.0 }, { it / 60.0 }),
            ConverterUnit("Hours", { it * 3_600.0 }, { it / 3_600.0 }),
            ConverterUnit("Days", { it * 86_400.0 }, { it / 86_400.0 }),
            ConverterUnit("Weeks", { it * 604_800.0 }, { it / 604_800.0 }),
        ),
        ConverterCategory.PRESSURE to listOf(
            ConverterUnit("Pascals", { it }, { it }),
            ConverterUnit("Kilopascals", { it * 1_000.0 }, { it / 1_000.0 }),
            ConverterUnit("Bar", { it * 100_000.0 }, { it / 100_000.0 }),
            ConverterUnit("PSI", { it * 6_894.757293 }, { it / 6_894.757293 }),
            ConverterUnit("Atmospheres", { it * 101_325.0 }, { it / 101_325.0 }),
        ),
        ConverterCategory.ENERGY to listOf(
            ConverterUnit("Joules", { it }, { it }),
            ConverterUnit("Kilojoules", { it * 1_000.0 }, { it / 1_000.0 }),
            ConverterUnit("Calories", { it * 4.184 }, { it / 4.184 }),
            ConverterUnit("Kilocalories", { it * 4_184.0 }, { it / 4_184.0 }),
            ConverterUnit("Watt-hours", { it * 3_600.0 }, { it / 3_600.0 }),
            ConverterUnit("Kilowatt-hours", { it * 3_600_000.0 }, { it / 3_600_000.0 }),
        ),
        ConverterCategory.POWER to listOf(
            ConverterUnit("Watts", { it }, { it }),
            ConverterUnit("Kilowatts", { it * 1_000.0 }, { it / 1_000.0 }),
            ConverterUnit("Horsepower", { it * 745.699872 }, { it / 745.699872 }),
        ),
        ConverterCategory.ANGLE to listOf(
            ConverterUnit("Degrees", { Math.toRadians(it) }, { Math.toDegrees(it) }),
            ConverterUnit("Radians", { it }, { it }),
            ConverterUnit("Gradians", { it * Math.PI / 200.0 }, { it * 200.0 / Math.PI }),
        ),
        ConverterCategory.STORAGE to listOf(
            ConverterUnit("Bytes", { it }, { it }),
            ConverterUnit("KB", { it * 1_024.0 }, { it / 1_024.0 }),
            ConverterUnit("MB", { it * 1_024.0 * 1_024.0 }, { it / (1_024.0 * 1_024.0) }),
            ConverterUnit("GB", { it * 1_024.0 * 1_024.0 * 1_024.0 }, { it / (1_024.0 * 1_024.0 * 1_024.0) }),
            ConverterUnit("TB", { it * 1_099_511_627_776.0 }, { it / 1_099_511_627_776.0 }),
        ),
    )

    fun convert(
        value: String,
        category: ConverterCategory,
        fromIndex: Int,
        toIndex: Int,
    ): String {
        val numericValue = value.toDoubleOrNull() ?: return ""
        val units = categories.getValue(category)
        val from = units.getOrNull(fromIndex) ?: return ""
        val to = units.getOrNull(toIndex) ?: return ""
        val baseValue = from.toBase(numericValue)
        val converted = to.fromBase(baseValue)
        return format(converted)
    }

    private fun format(value: Double): String {
        if (!value.isFinite()) return ""
        return BigDecimal.valueOf(value)
            .setScale(4, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }
}
