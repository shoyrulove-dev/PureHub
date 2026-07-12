package com.purehub.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorHex: String = "#7A9E7E",
    val targetDaysPerWeek: Int = 7,
    val createdAtEpochMillis: Long,
    val isArchived: Boolean = false,
)
