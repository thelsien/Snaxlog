package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import kotlinx.coroutines.flow.Flow

interface CalorieGoalRepository {
    fun getActiveGoal(): Flow<CalorieGoalEntity?>
    fun getAllGoals(): Flow<List<CalorieGoalEntity>>
    suspend fun getGoalById(id: Long): CalorieGoalEntity?
    suspend fun setActiveGoal(goalId: Long)
    suspend fun addGoal(goal: CalorieGoalEntity): Long
    suspend fun updateGoal(goal: CalorieGoalEntity)
    suspend fun deleteGoal(goalId: Long)
}
