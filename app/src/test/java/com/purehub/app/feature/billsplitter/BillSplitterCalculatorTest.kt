package com.purehub.app.feature.billsplitter

import org.junit.Assert.assertEquals
import org.junit.Test

class BillSplitterCalculatorTest {
    @Test
    fun calculatesAssignedAndSharedTotals() {
        val summary = BillSplitterCalculator.calculate(
            totalBill = 30.0,
            taxAmount = 3.0,
            tipAmount = 6.0,
            peopleCount = 3,
            items = listOf(
                BillLineItem(1, "Pizza", 18.0, setOf(0, 1)),
                BillLineItem(2, "Drink", 6.0, setOf(2)),
            ),
        )

        assertEquals(39.0, summary.overallTotal, 0.001)
        assertEquals(3, summary.people.size)
    }
}
