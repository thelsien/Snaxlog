package com.snaxlog.app.data.local.database

import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.dao.FoodDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles one-time database seeding on first launch.
 * Inserts the comprehensive pre-loaded food database (500+ items)
 * and pre-defined calorie goals.
 *
 * EPIC-003 / US-011 / AC-051: Database is pre-populated and immediately
 * available without internet connection on first install.
 */
@Singleton
class DatabaseInitializer @Inject constructor(
    private val foodDao: FoodDao,
    private val calorieGoalDao: CalorieGoalDao
) {
    suspend fun initializeIfNeeded() {
        if (foodDao.getFoodCount() == 0) {
            foodDao.insertAll(SnaxlogDatabase.getPreloadedFoods())
        }
        if (calorieGoalDao.getGoalCount() == 0) {
            calorieGoalDao.insertAll(SnaxlogDatabase.getPredefinedGoals())
        }
    }
}
