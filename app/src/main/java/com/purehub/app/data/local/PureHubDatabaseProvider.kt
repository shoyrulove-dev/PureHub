package com.purehub.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DATABASE_NAME = "purehub.db"

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense_entries ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'expense'")
        db.execSQL("ALTER TABLE expense_entries ADD COLUMN wallet TEXT NOT NULL DEFAULT 'Cash'")
    }
}

object PureHubDatabaseProvider {
    @Volatile
    private var instance: PureHubDatabase? = null

    fun get(context: Context): PureHubDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PureHubDatabase::class.java,
                DATABASE_NAME,
            ).addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
