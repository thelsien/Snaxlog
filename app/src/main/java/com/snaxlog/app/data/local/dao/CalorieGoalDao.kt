package com.snaxlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieGoalDao {

    @Query("SELECT * FROM calorie_goals ORDER BY isPredefined DESC, name")
    fun getAllGoals(): Flow<List<CalorieGoalEntity>>

    @Query("SELECT * FROM calorie_goals WHERE isActive = 1 LIMIT 1")
    fun getActiveGoal(): Flow<CalorieGoalEntity?>

    @Query("SELECT * FROM calorie_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): CalorieGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: CalorieGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<CalorieGoalEntity>)

    @Update
    suspend fun updateGoal(goal: CalorieGoalEntity)

    @Query("DELETE FROM calorie_goals WHERE id = :id AND isPredefined = 0")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE calorie_goals SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllGoals()

    @Query("UPDATE calorie_goals SET isActive = 1 WHERE id = :id")
    suspend fun activateGoal(id: Long)

    @Query("SELECT COUNT(*) FROM calorie_goals")
    suspend fun getGoalCount(): Int
}
