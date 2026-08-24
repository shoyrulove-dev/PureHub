package com.purehub.app.feature.expense

import com.purehub.app.data.local.entity.ExpenseEntryEntity
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseInsightsTest {
    @Test
    fun calculatesMonthlyAndCategoryTotals() {
        val expenses = listOf(
            summary("Coffee", 450, "Dining", 1_788_220_800_000),
            summary("Train", 1200, "Transport", 1_788_307_200_000),
        )
        val result = ExpenseInsights.calculate(expenses, 1_788_393_600_000, ZoneOffset.UTC)
        assertEquals(1650, result.currentMonthMinor)
        assertEquals("Transport", result.categoryTotals.first().category)
    }

    @Test
    fun csvEscapesUserContent() {
        val csv = ExpenseInsights.toCsv(listOf(summary("Coffee, tea", 450, "Dining", 1)))
        assertTrue(csv.contains("\"Coffee, tea\""))
        assertTrue(csv.startsWith("title,amount"))
    }

    private fun summary(title: String, amount: Long, category: String, timestamp: Long) = ExpenseSummary(
        ExpenseEntryEntity(
            title = title,
            amountMinor = amount,
            category = category,
            happenedAtEpochMillis = timestamp,
            createdAtEpochMillis = timestamp,
        ),
        "%.2f".format(amount / 100.0),
    )
}
