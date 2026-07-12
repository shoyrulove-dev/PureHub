package com.purehub.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.purehub.app.data.local.entity.HabitCheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCheckInDao {
    @Query("SELECT * FROM habit_check_ins ORDER BY completedOn DESC")
    fun observeAllCheckIns(): Flow<List<HabitCheckInEntity>>

    @Query(
        """
        SELECT * FROM habit_check_ins
        WHERE habitId = :habitId
        ORDER BY completedOn DESC
        """,
    )
    fun observeCheckInsForHabit(habitId: Long): Flow<List<HabitCheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheckIn(checkIn: HabitCheckInEntity): Long

    @Query("DELETE FROM habit_check_ins WHERE habitId = :habitId AND completedOn = :completedOn")
    suspend fun deleteCheckIn(habitId: Long, completedOn: String)
}
