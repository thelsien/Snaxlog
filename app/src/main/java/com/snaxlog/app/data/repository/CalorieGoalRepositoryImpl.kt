package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalorieGoalRepositoryImpl @Inject constructor(
    private val calorieGoalDao: CalorieGoalDao
) : CalorieGoalRepository {

    override fun getActiveGoal(): Flow<CalorieGoalEntity?> =
        calorieGoalDao.getActiveGoal()

    override fun getAllGoals(): Flow<List<CalorieGoalEntity>> =
        calorieGoalDao.getAllGoals()

    override suspend fun setActiveGoal(goalId: Long) {
        calorieGoalDao.deactivateAllGoals()
        calorieGoalDao.activateGoal(goalId)
    }
}
