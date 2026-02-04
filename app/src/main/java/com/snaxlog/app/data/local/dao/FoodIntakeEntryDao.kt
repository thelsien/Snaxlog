package com.snaxlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodIntakeEntryDao {

    @Transaction
    @Query(
        """
        SELECT * FROM food_intake_entries
        WHERE date = :date
        ORDER BY timestamp DESC
        """
    )
    fun getEntriesForDate(date: String): Flow<List<FoodIntakeWithFood>>

    @Query("SELECT * FROM food_intake_entries WHERE id = :id")
    suspend fun getEntryById(id: Long): FoodIntakeEntryEntity?

    @Transaction
    @Query("SELECT * FROM food_intake_entries WHERE id = :id")
    suspend fun getEntryWithFoodById(id: Long): FoodIntakeWithFood?

    @Insert
    suspend fun insertEntry(entry: FoodIntakeEntryEntity): Long

    @Update
    suspend fun updateEntry(entry: FoodIntakeEntryEntity)

    @Query("DELETE FROM food_intake_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)

    @Query(
        """
        SELECT COALESCE(SUM(totalCalories), 0) FROM food_intake_entries
        WHERE date = :date
        """
    )
    fun getTotalCaloriesForDate(date: String): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(totalProtein), 0.0) FROM food_intake_entries
        WHERE date = :date
        """
    )
    fun getTotalProteinForDate(date: String): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(totalFat), 0.0) FROM food_intake_entries
        WHERE date = :date
        """
    )
    fun getTotalFatForDate(date: String): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(totalCarbs), 0.0) FROM food_intake_entries
        WHERE date = :date
        """
    )
    fun getTotalCarbsForDate(date: String): Flow<Double>
}
