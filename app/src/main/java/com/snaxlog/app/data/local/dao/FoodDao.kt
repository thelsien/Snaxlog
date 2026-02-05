package com.snaxlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.snaxlog.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY category, name")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodEntity?

    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name
        """
    )
    fun searchFoods(query: String): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int
}
