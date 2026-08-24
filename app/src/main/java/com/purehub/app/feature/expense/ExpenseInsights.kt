package com.purehub.app.feature.expense

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class ExpenseCategoryTotal(val category: String, val amountMinor: Long)

data class ExpenseInsightSnapshot(
    val allTimeMinor: Long,
    val currentMonthMinor: Long,
    val categoryTotals: List<ExpenseCategoryTotal>,
)

object ExpenseInsights {
    fun calculate(
        expenses: List<ExpenseSummary>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ExpenseInsightSnapshot {
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        val monthTotal = expenses.filter {
            YearMonth.from(Instant.ofEpochMilli(it.entry.happenedAtEpochMillis).atZone(zoneId)) == currentMonth
        }.sumOf { it.entry.amountMinor }
        return ExpenseInsightSnapshot(
            allTimeMinor = expenses.sumOf { it.entry.amountMinor },
            currentMonthMinor = monthTotal,
            categoryTotals = expenses
                .groupBy { it.entry.category.ifBlank { "General" } }
                .map { (category, values) -> ExpenseCategoryTotal(category, values.sumOf { it.entry.amountMinor }) }
                .sortedByDescending { it.amountMinor },
        )
    }

    fun toCsv(expenses: List<ExpenseSummary>): String = buildString {
        appendLine("title,amount,category,note,timestamp")
        expenses.forEach { summary ->
            val entry = summary.entry
            appendLine(
                listOf(entry.title, summary.amountDisplay, entry.category, entry.note, entry.happenedAtEpochMillis.toString())
                    .joinToString(",") { csvCell(it) },
            )
        }
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
