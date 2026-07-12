package com.purehub.app.data.local

import android.content.Context
import androidx.room.Room

private const val DATABASE_NAME = "purehub.db"

object PureHubDatabaseProvider {
    @Volatile
    private var instance: PureHubDatabase? = null

    fun get(context: Context): PureHubDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                PureHubDatabase::class.java,
                DATABASE_NAME,
            ).fallbackToDestructiveMigration(true)
                .build()
                .also { instance = it }
        }
    }
}
