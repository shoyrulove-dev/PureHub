package com.purehub.app.feature.expense

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class ExpenseCategoryTotal(val category: String, val amountMinor: Long)

data class ExpenseInsightSnapshot(
    val allTimeExpenseMinor: Long,
    val currentMonthExpenseMinor: Long,
    val currentMonthIncomeMinor: Long,
    val currentMonthNetMinor: Long,
    val categoryTotals: List<ExpenseCategoryTotal>,
)

object ExpenseInsights {
    fun calculate(
        expenses: List<ExpenseSummary>,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ExpenseInsightSnapshot {
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        val currentMonthRows = expenses.filter {
            YearMonth.from(Instant.ofEpochMilli(it.entry.happenedAtEpochMillis).atZone(zoneId)) == currentMonth
        }
        val monthExpense = currentMonthRows.filter { it.entry.transactionType != "income" }.sumOf { it.entry.amountMinor }
        val monthIncome = currentMonthRows.filter { it.entry.transactionType == "income" }.sumOf { it.entry.amountMinor }
        return ExpenseInsightSnapshot(
            allTimeExpenseMinor = expenses.filter { it.entry.transactionType != "income" }.sumOf { it.entry.amountMinor },
            currentMonthExpenseMinor = monthExpense,
            currentMonthIncomeMinor = monthIncome,
            currentMonthNetMinor = monthIncome - monthExpense,
            categoryTotals = currentMonthRows
                .filter { it.entry.transactionType != "income" }
                .groupBy { it.entry.category.ifBlank { "General" } }
                .map { (category, values) -> ExpenseCategoryTotal(category, values.sumOf { it.entry.amountMinor }) }
                .sortedByDescending { it.amountMinor },
        )
    }

    fun toCsv(expenses: List<ExpenseSummary>): String = buildString {
        appendLine("title,type,amount,category,wallet,note,timestamp")
        expenses.forEach { summary ->
            val entry = summary.entry
            appendLine(
                listOf(entry.title, entry.transactionType, summary.amountDisplay, entry.category, entry.wallet, entry.note, entry.happenedAtEpochMillis.toString())
                    .joinToString(",") { csvCell(it) },
            )
        }
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
