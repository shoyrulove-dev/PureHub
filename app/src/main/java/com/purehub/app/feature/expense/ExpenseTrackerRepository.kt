package com.purehub.app.feature.expense

import com.purehub.app.data.local.dao.ExpenseDao
import com.purehub.app.data.local.entity.ExpenseEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ExpenseSummary(
    val entry: ExpenseEntryEntity,
    val amountDisplay: String,
)

class ExpenseTrackerRepository(
    private val expenseDao: ExpenseDao,
) {
    fun observeExpenses(): Flow<List<ExpenseSummary>> {
        return expenseDao.observeExpenses().map { expenses ->
            expenses.map { entry ->
                ExpenseSummary(
                    entry = entry,
                    amountDisplay = "%.2f".format(entry.amountMinor / 100.0),
                )
            }
        }
    }

    suspend fun addExpense(
        title: String,
        amountText: String,
        category: String,
        note: String,
    ) {
        val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
        if (title.isBlank() || amountMinor <= 0L) return

        val now = System.currentTimeMillis()
        expenseDao.insertExpense(
            ExpenseEntryEntity(
                title = title.trim(),
                amountMinor = amountMinor,
                category = category.trim().ifBlank { "General" },
                note = note.trim(),
                happenedAtEpochMillis = now,
                createdAtEpochMillis = now,
            ),
        )
    }

    suspend fun deleteExpense(entry: ExpenseEntryEntity) {
        expenseDao.deleteExpense(entry)
    }
}
