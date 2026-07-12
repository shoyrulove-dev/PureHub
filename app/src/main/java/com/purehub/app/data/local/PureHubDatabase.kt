package com.purehub.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.purehub.app.data.local.dao.ExpenseDao
import com.purehub.app.data.local.dao.HabitCheckInDao
import com.purehub.app.data.local.dao.HabitDao
import com.purehub.app.data.local.entity.ExpenseEntryEntity
import com.purehub.app.data.local.entity.HabitCheckInEntity
import com.purehub.app.data.local.entity.HabitEntity

@Database(
    entities = [
        HabitEntity::class,
        HabitCheckInEntity::class,
        ExpenseEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PureHubDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun habitCheckInDao(): HabitCheckInDao

    abstract fun expenseDao(): ExpenseDao
}
