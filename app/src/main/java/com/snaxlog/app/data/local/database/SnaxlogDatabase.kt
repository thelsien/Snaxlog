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
    version = 1,
    exportSchema = false
)
abstract class SnaxlogDatabase : RoomDatabase() {

    abstract fun foodDao(): FoodDao
    abstract fun foodIntakeEntryDao(): FoodIntakeEntryDao
    abstract fun calorieGoalDao(): CalorieGoalDao

    companion object {
        const val DATABASE_NAME = "snaxlog_database"

        /**
         * Pre-populated dummy food data for initial development and testing.
         * Per requirements (FIP-003 Phase 1): Apple, Chicken Breast, White Rice.
         */
        fun getDummyFoods(): List<FoodEntity> = listOf(
            FoodEntity(
                id = 1,
                name = "Apple",
                category = "Fruits",
                servingSize = "1 medium (182g)",
                servingWeightGrams = 182.0,
                caloriesPerServing = 95,
                proteinPerServing = 0.5,
                fatPerServing = 0.3,
                carbsPerServing = 25.1
            ),
            FoodEntity(
                id = 2,
                name = "Grilled Chicken Breast",
                category = "Protein",
                servingSize = "1 breast (100g)",
                servingWeightGrams = 100.0,
                caloriesPerServing = 165,
                proteinPerServing = 31.0,
                fatPerServing = 3.6,
                carbsPerServing = 0.0
            ),
            FoodEntity(
                id = 3,
                name = "White Rice",
                category = "Grains",
                servingSize = "1 cup cooked (158g)",
                servingWeightGrams = 158.0,
                caloriesPerServing = 206,
                proteinPerServing = 4.3,
                fatPerServing = 0.4,
                carbsPerServing = 44.5
            )
        )

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
