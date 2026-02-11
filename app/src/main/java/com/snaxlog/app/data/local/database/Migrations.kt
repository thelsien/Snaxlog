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

    /**
     * Migration from version 3 to 4.
     * EPIC-006: User-Created Foods and Recipes
     *
     * Changes:
     * 1. Adds new columns to foods table for custom food support
     * 2. Creates recipe_ingredients table for recipe ingredients
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to foods table
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN isUserCreated INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN foodType TEXT NOT NULL DEFAULT 'PREDEFINED'"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN servingUnit TEXT NOT NULL DEFAULT 'GRAM'"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN servingSizeValue REAL NOT NULL DEFAULT 0.0"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN numberOfServings REAL DEFAULT NULL"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN createdAt INTEGER DEFAULT NULL"
            )
            database.execSQL(
                "ALTER TABLE foods ADD COLUMN updatedAt INTEGER DEFAULT NULL"
            )

            // Create recipe_ingredients table
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recipe_ingredients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    recipeId INTEGER NOT NULL,
                    ingredientFoodId INTEGER NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (recipeId) REFERENCES foods(id) ON DELETE CASCADE,
                    FOREIGN KEY (ingredientFoodId) REFERENCES foods(id) ON DELETE NO ACTION
                )
                """.trimIndent()
            )

            // Create indices for recipe_ingredients
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recipe_ingredients_recipeId ON recipe_ingredients(recipeId)"
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recipe_ingredients_ingredientFoodId ON recipe_ingredients(ingredientFoodId)"
            )
        }
    }
}
