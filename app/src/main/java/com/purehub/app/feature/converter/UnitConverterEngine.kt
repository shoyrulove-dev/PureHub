package com.purehub.app.feature.converter

import kotlin.math.roundToInt

enum class ConverterCategory(val title: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
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
        ConverterCategory.STORAGE to listOf(
            ConverterUnit("Bytes", { it }, { it }),
            ConverterUnit("KB", { it * 1_024.0 }, { it / 1_024.0 }),
            ConverterUnit("MB", { it * 1_024.0 * 1_024.0 }, { it / (1_024.0 * 1_024.0) }),
            ConverterUnit("GB", { it * 1_024.0 * 1_024.0 * 1_024.0 }, { it / (1_024.0 * 1_024.0 * 1_024.0) }),
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
        val baseValue = units[fromIndex].toBase(numericValue)
        val converted = units[toIndex].fromBase(baseValue)
        return format(converted)
    }

    private fun format(value: Double): String {
        val rounded = (value * 10_000.0).roundToInt() / 10_000.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
