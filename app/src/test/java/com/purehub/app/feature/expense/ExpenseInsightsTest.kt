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
        assertEquals(1650, result.currentMonthExpenseMinor)
        assertEquals(-1650, result.currentMonthNetMinor)
        assertEquals("Transport", result.categoryTotals.first().category)
    }

    @Test
    fun csvEscapesUserContent() {
        val csv = ExpenseInsights.toCsv(listOf(summary("Coffee, tea", 450, "Dining", 1)))
        assertTrue(csv.contains("\"Coffee, tea\""))
        assertTrue(csv.startsWith("title,type,amount"))

    }

    @Test
    fun separatesIncomeFromExpenses() {
        val rows = listOf(
            summary("Salary", 500_000, "Income", 1_788_220_800_000, "income"),
            summary("Rent", 120_000, "Housing", 1_788_307_200_000),
        )
        val result = ExpenseInsights.calculate(rows, 1_788_393_600_000, ZoneOffset.UTC)
        assertEquals(500_000, result.currentMonthIncomeMinor)
        assertEquals(120_000, result.currentMonthExpenseMinor)
        assertEquals(380_000, result.currentMonthNetMinor)
    }

    private fun summary(title: String, amount: Long, category: String, timestamp: Long, type: String = "expense") = ExpenseSummary(
        ExpenseEntryEntity(
            title = title,
            amountMinor = amount,
            category = category,
            transactionType = type,
            happenedAtEpochMillis = timestamp,
            createdAtEpochMillis = timestamp,
        ),
        "%.2f".format(amount / 100.0),
    )
}
