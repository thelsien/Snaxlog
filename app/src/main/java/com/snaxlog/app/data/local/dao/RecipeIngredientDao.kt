package com.snaxlog.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.snaxlog.app.data.local.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.Flow

/**
 * EPIC-006: User-Created Foods and Recipes
 * US-019: Create Recipe with Multiple Ingredients
 *
 * Data Access Object for recipe ingredients.
 * Handles CRUD operations for ingredients within recipes.
 */
@Dao
interface RecipeIngredientDao {

    /**
     * Inserts a single recipe ingredient.
     *
     * @param ingredient The ingredient to insert
     * @return The row ID of the inserted ingredient
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: RecipeIngredientEntity): Long

    /**
     * Inserts multiple recipe ingredients.
     *
     * @param ingredients The list of ingredients to insert
     * @return List of row IDs of the inserted ingredients
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<RecipeIngredientEntity>): List<Long>

    /**
     * Updates an existing recipe ingredient.
     *
     * @param ingredient The ingredient to update
     */
    @Update
    suspend fun update(ingredient: RecipeIngredientEntity)

    /**
     * Deletes a recipe ingredient.
     *
     * @param ingredient The ingredient to delete
     */
    @Delete
    suspend fun delete(ingredient: RecipeIngredientEntity)

    /**
     * Deletes a recipe ingredient by ID.
     *
     * @param id The ID of the ingredient to delete
     */
    @Query("DELETE FROM recipe_ingredients WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Deletes all ingredients for a specific recipe.
     * Used when deleting or rebuilding a recipe's ingredient list.
     *
     * @param recipeId The ID of the recipe
     */
    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteAllForRecipe(recipeId: Long)

    /**
     * Gets all ingredients for a specific recipe, ordered by sortOrder.
     *
     * @param recipeId The ID of the recipe
     * @return Flow of list of ingredients for the recipe
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    fun getIngredientsForRecipe(recipeId: Long): Flow<List<RecipeIngredientEntity>>

    /**
     * Gets all ingredients for a specific recipe (one-time query).
     *
     * @param recipeId The ID of the recipe
     * @return List of ingredients for the recipe
     */
    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY sortOrder")
    suspend fun getIngredientsForRecipeOnce(recipeId: Long): List<RecipeIngredientEntity>

    /**
     * Counts the number of recipes that use a specific food as an ingredient.
     * Used to show warnings when editing/deleting foods used in recipes.
     * EC-016-003: Check if food is used in recipes before editing.
     * EC-017-003: Check if food is used in recipes before deleting.
     *
     * @param foodId The ID of the food to check
     * @return Number of recipes using this food as an ingredient
     */
    @Query("SELECT COUNT(DISTINCT recipeId) FROM recipe_ingredients WHERE ingredientFoodId = :foodId")
    suspend fun countRecipesUsingFood(foodId: Long): Int

    /**
     * Gets the IDs of all recipes that use a specific food as an ingredient.
     *
     * @param foodId The ID of the food
     * @return List of recipe IDs using this food
     */
    @Query("SELECT DISTINCT recipeId FROM recipe_ingredients WHERE ingredientFoodId = :foodId")
    suspend fun getRecipeIdsUsingFood(foodId: Long): List<Long>

    /**
     * Gets the count of ingredients for a recipe.
     * Used for validation (recipes must have at least one ingredient).
     *
     * @param recipeId The ID of the recipe
     * @return Number of ingredients in the recipe
     */
    @Query("SELECT COUNT(*) FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun getIngredientCount(recipeId: Long): Int

    /**
     * Updates the sort order of ingredients.
     * Used when reordering ingredients in a recipe.
     *
     * @param id The ID of the ingredient
     * @param sortOrder The new sort order
     */
    @Query("UPDATE recipe_ingredients SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
