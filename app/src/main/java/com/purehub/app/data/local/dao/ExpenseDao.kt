package com.purehub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.purehub.app.data.local.entity.ExpenseEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_entries ORDER BY happenedAtEpochMillis DESC, createdAtEpochMillis DESC")
    fun observeExpenses(): Flow<List<ExpenseEntryEntity>>

    @Query(
        """
        SELECT * FROM expense_entries
        WHERE happenedAtEpochMillis BETWEEN :fromEpochMillis AND :toEpochMillis
        ORDER BY happenedAtEpochMillis DESC
        """,
    )
    fun observeExpensesBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): Flow<List<ExpenseEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntryEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntryEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntryEntity)
}
