package com.purehub.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expense_entries",
    indices = [
        Index(value = ["happenedAtEpochMillis"]),
        Index(value = ["category"]),
    ],
)
data class ExpenseEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amountMinor: Long,
    val category: String,
    val note: String = "",
    val happenedAtEpochMillis: Long,
    val createdAtEpochMillis: Long,
)
