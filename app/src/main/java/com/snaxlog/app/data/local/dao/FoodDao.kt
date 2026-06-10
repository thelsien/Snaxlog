package com.snaxlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for foods.
 * EPIC-003: Pre-loaded Food Database
 * EPIC-006: User-Created Foods and Recipes
 */
@Dao
interface FoodDao {

    // ============================
    // Original EPIC-003 queries
    // ============================

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

    @Query("SELECT DISTINCT category FROM foods ORDER BY category")
    fun getDistinctCategories(): Flow<List<String>>

    @Query("SELECT * FROM foods WHERE category = :category ORDER BY name")
    fun getFoodsByCategory(category: String): Flow<List<FoodEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int

    // ============================
    // EPIC-006: Custom food queries
    // ============================

    /**
     * Inserts a single food item and returns its ID.
     * Used for creating custom foods and recipes.
     * US-018, US-019
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    /**
     * Updates a food item.
     * Used for editing custom foods and recipes.
     * US-021
     */
    @Update
    suspend fun update(food: FoodEntity)

    /**
     * Deletes a food item.
     * Used for deleting custom foods and recipes.
     * US-022
     */
    @Delete
    suspend fun delete(food: FoodEntity)

    /**
     * Deletes a food item by ID.
     * US-022
     */
    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Gets all user-created foods (both simple foods and recipes).
     * US-020: View and Use Custom Foods in Food Search
     */
    @Query("SELECT * FROM foods WHERE isUserCreated = 1 ORDER BY name")
    fun getAllUserCreatedFoods(): Flow<List<FoodEntity>>

    /**
     * Gets all user-created simple foods (not recipes).
     */
    @Query("SELECT * FROM foods WHERE isUserCreated = 1 AND foodType = 'SIMPLE' ORDER BY name")
    fun getAllSimpleCustomFoods(): Flow<List<FoodEntity>>

    /**
     * Gets all user-created recipes.
     */
    @Query("SELECT * FROM foods WHERE isUserCreated = 1 AND foodType = 'RECIPE' ORDER BY name")
    fun getAllRecipes(): Flow<List<FoodEntity>>

    /**
     * Searches all foods including custom foods, with badges.
     * Results are sorted alphabetically (custom foods mixed with pre-loaded).
     * US-020: AC-015-002 - Custom foods sorted alphabetically alongside pre-loaded foods
     */
    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name
        """
    )
    fun searchAllFoods(query: String): Flow<List<FoodEntity>>

    /**
     * Searches foods excluding recipes.
     * Used when selecting ingredients for a recipe.
     * EC-014-001: Recipes cannot contain other recipes (prevents circular dependencies)
     */
    @Query(
        """
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%'
          AND foodType != 'RECIPE'
        ORDER BY
            CASE WHEN name LIKE :query || '%' THEN 0 ELSE 1 END,
            name
        """
    )
    fun searchFoodsExcludingRecipes(query: String): Flow<List<FoodEntity>>

    /**
     * Gets all foods excluding recipes (for ingredient selection).
     * EC-014-001: Recipes cannot contain other recipes
     */
    @Query("SELECT * FROM foods WHERE foodType != 'RECIPE' ORDER BY category, name")
    fun getAllFoodsExcludingRecipes(): Flow<List<FoodEntity>>

    /**
     * Checks if a food with the given name already exists.
     * Used for duplicate name warnings.
     * EC-013-007: Warning when creating custom food with same name as existing food
     */
    @Query("SELECT COUNT(*) FROM foods WHERE LOWER(name) = LOWER(:name)")
    suspend fun countFoodsWithName(name: String): Int

    /**
     * Checks if a custom food is currently used in any intake logs.
     * Used for showing warnings when editing/deleting.
     * EC-016-002: Warning when editing food that is in logs
     */
    @Query(
        """
        SELECT COUNT(*) FROM food_intake_entries
        WHERE foodId = :foodId
        """
    )
    suspend fun countIntakeEntriesForFood(foodId: Long): Int

    /**
     * Gets the count of user-created foods.
     */
    @Query("SELECT COUNT(*) FROM foods WHERE isUserCreated = 1")
    suspend fun getCustomFoodCount(): Int

    /**
     * Gets multiple foods by their IDs.
     * Used for loading recipe ingredients.
     */
    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getFoodsByIds(ids: List<Long>): List<FoodEntity>
}
