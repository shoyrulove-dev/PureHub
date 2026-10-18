package com.purehub.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PureHubDatabaseMigrationTest {
    private val databaseName = "purehub-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PureHubDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate1To2PreservesExpensesAndAddsSafeDefaults() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                "INSERT INTO expense_entries (id, title, amountMinor, category, note, happenedAtEpochMillis, createdAtEpochMillis) VALUES (1, 'Lunch', 125000, 'Food', 'Team lunch', 1000, 1000)",
            )
            close()
        }

        helper.runMigrationsAndValidate(databaseName, 2, true, MIGRATION_1_2).use { database ->
            database.query(
                "SELECT title, amountMinor, category, note, transactionType, wallet FROM expense_entries WHERE id = 1",
            ).use { cursor ->
                check(cursor.moveToFirst())
                assertEquals("Lunch", cursor.getString(0))
                assertEquals(125000L, cursor.getLong(1))
                assertEquals("Food", cursor.getString(2))
                assertEquals("Team lunch", cursor.getString(3))
                assertEquals("expense", cursor.getString(4))
                assertEquals("Cash", cursor.getString(5))
            }
        }
    }
}
