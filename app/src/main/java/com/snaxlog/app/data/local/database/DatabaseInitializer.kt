package com.snaxlog.app.data.local.database

import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.dao.FoodDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles one-time database seeding on first launch.
 * Inserts the dummy food database and pre-defined calorie goals.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val foodDao: FoodDao,
    private val calorieGoalDao: CalorieGoalDao
) {
    suspend fun initializeIfNeeded() {
        if (foodDao.getFoodCount() == 0) {
            foodDao.insertAll(SnaxlogDatabase.getDummyFoods())
        }
        if (calorieGoalDao.getGoalCount() == 0) {
            calorieGoalDao.insertAll(SnaxlogDatabase.getPredefinedGoals())
        }
    }
}
