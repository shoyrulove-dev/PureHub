package com.purehub.app.feature.billsplitter

import org.junit.Assert.assertEquals
import org.junit.Test

class BillPresetCodecTest {
    @Test
    fun roundTripsPresetJson() {
        val presets = listOf(
            BillPreset(
                name = "Dinner",
                totalBill = "40",
                taxAmount = "4",
                tipAmount = "6",
                peopleCount = 2,
                items = listOf(
                    BillPresetItem("Soup", "10", listOf(0)),
                    BillPresetItem("Cake", "12", listOf(1)),
                ),
            ),
        )

        val encoded = BillPresetCodec.encode(presets)
        val decoded = BillPresetCodec.decode(encoded)

        assertEquals(presets, decoded)
    }
}
