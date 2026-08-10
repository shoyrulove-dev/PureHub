package com.purehub.app.feature.receipt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {
    @Test
    fun parsesEnglishReceiptTotalsAndItems() {
        val result = ReceiptParser.parse("""
            PURE CAFE
            Coffee 4.50
            Sandwich 8.00
            Tax 1.25
            Total 13.75
            10/08/2026
        """.trimIndent())
        assertEquals("PURE CAFE", result.merchant)
        assertEquals(13.75, result.total ?: 0.0, 0.001)
        assertEquals(1.25, result.tax ?: 0.0, 0.001)
        assertEquals(2, result.lines.size)
    }

    @Test
    fun parsesVietnameseTotal() {
        val result = ReceiptParser.parse("CỬA HÀNG PURE\nTổng cộng 125.000 VND")
        assertEquals(125000.0, result.total ?: 0.0, 0.001)
        assertTrue(result.rawText.contains("Tổng cộng"))
    }

    @Test
    fun pairsSplitOcrColumnsAndIgnoresReceiptReference() {
        val result = ReceiptParser.parse("""
            PUREHUB TEST STORE
            Receipt #BETA15
            2026-08-10
            Coffee
            Notebook
            USB Cable
            Subtotal
            Tax
            TOTAL
            4.50
            12.00
            8.00
            24.50
            2.45
            26.95
        """.trimIndent())

        assertEquals(26.95, result.total ?: 0.0, 0.001)
        assertEquals(2.45, result.tax ?: 0.0, 0.001)
        assertEquals(listOf("Coffee", "Notebook", "USB Cable", "Subtotal"), result.lines.map { it.name })
        assertEquals(listOf(4.50, 12.00, 8.00, 24.50), result.lines.map { it.amount })
    }
}
