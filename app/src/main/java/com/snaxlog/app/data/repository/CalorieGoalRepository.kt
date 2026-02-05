package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import kotlinx.coroutines.flow.Flow

interface CalorieGoalRepository {
    fun getActiveGoal(): Flow<CalorieGoalEntity?>
    fun getAllGoals(): Flow<List<CalorieGoalEntity>>
    suspend fun setActiveGoal(goalId: Long)
}
