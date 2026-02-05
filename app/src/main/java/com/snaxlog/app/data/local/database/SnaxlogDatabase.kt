package com.snaxlog.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.dao.FoodIntakeEntryDao
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodEntity::class,
        FoodIntakeEntryEntity::class,
        CalorieGoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SnaxlogDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun foodIntakeEntryDao(): FoodIntakeEntryDao
    abstract fun calorieGoalDao(): CalorieGoalDao

    companion object {
        const val DATABASE_NAME = "snaxlog_database"

        /**
         * Returns the comprehensive pre-loaded food database (500+ items).
         * EPIC-003 / US-011: Full food database across 10 categories.
         * Replaces the former dummy 3-item database from FIP-003 Phase 1.
         */
        fun getPreloadedFoods(): List<FoodEntity> = FoodDatabaseProvider.getAllFoods()

        /**
         * Pre-defined calorie goals that cannot be edited or deleted by users.
         */
        fun getPredefinedGoals(): List<CalorieGoalEntity> = listOf(
            CalorieGoalEntity(
                id = 1,
                name = "Weight Loss",
                calorieTarget = 1500,
                proteinTarget = 120.0,
                fatTarget = 50.0,
                carbsTarget = 150.0,
                isPredefined = true
            ),
            CalorieGoalEntity(
                id = 2,
                name = "Maintenance",
                calorieTarget = 2000,
                proteinTarget = 150.0,
                fatTarget = 67.0,
                carbsTarget = 200.0,
                isPredefined = true
            ),
            CalorieGoalEntity(
                id = 3,
                name = "Muscle Gain",
                calorieTarget = 2500,
                proteinTarget = 200.0,
                fatTarget = 83.0,
                carbsTarget = 250.0,
                isPredefined = true
            )
        )

        fun createCallback(scope: CoroutineScope): Callback {
            return object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    scope.launch(Dispatchers.IO) {
                        // This will be handled by the DatabaseInitializer
                    }
                }
            }
        }
    }
}
