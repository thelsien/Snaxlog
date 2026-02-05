package com.snaxlog.app.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for Snaxlog.
 */
object Migrations {

    /**
     * Migration from version 2 to 3.
     * FIP-005: Adds mealCategory column to food_intake_entries table.
     *
     * The mealCategory column is nullable (TEXT), defaulting to NULL.
     * Existing entries will have NULL mealCategory (uncategorized).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE food_intake_entries ADD COLUMN mealCategory TEXT DEFAULT NULL"
            )
        }
    }
}
