package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.RecipeWithIngredients
import com.snaxlog.app.data.local.entity.ServingUnit
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for food data operations.
 * EPIC-003: Pre-loaded Food Database
 * EPIC-006: User-Created Foods and Recipes
 */
interface FoodRepository {

    // ============================
    // Original EPIC-003 operations
    // ============================

    fun getAllFoods(): Flow<List<FoodEntity>>
    fun searchFoods(query: String): Flow<List<FoodEntity>>
    suspend fun getFoodById(id: Long): FoodEntity?
    fun getDistinctCategories(): Flow<List<String>>
    fun getFoodsByCategory(category: String): Flow<List<FoodEntity>>

    // ============================
    // EPIC-006: Custom Food operations
    // ============================

    /**
     * Creates a simple custom food.
     * US-018: Create Simple Custom Food
     *
     * @param name Food name (max 100 chars)
     * @param servingSizeValue Numeric serving size (> 0)
     * @param servingUnit Unit of measurement
     * @param protein Protein in grams per serving (>= 0)
     * @param fat Fat in grams per serving (>= 0)
     * @param carbs Carbs in grams per serving (>= 0)
     * @return The created food entity with generated ID
     */
    suspend fun createCustomFood(
        name: String,
        servingSizeValue: Double,
        servingUnit: ServingUnit,
        protein: Double,
        fat: Double,
        carbs: Double
    ): FoodEntity

    /**
     * Creates a recipe with multiple ingredients.
     * US-019: Create Recipe with Multiple Ingredients
     *
     * @param name Recipe name (max 100 chars)
     * @param numberOfServings How many servings the recipe makes (> 0)
     * @param ingredients List of ingredient specifications
     * @return The created recipe entity with calculated nutrition
     */
    suspend fun createRecipe(
        name: String,
        numberOfServings: Double,
        ingredients: List<RecipeIngredientInput>
    ): FoodEntity

    /**
     * Updates an existing custom food.
     * US-021: Edit Custom Foods and Recipes
     *
     * @param foodId ID of the food to update
     * @param name Updated name
     * @param servingSizeValue Updated serving size
     * @param servingUnit Updated unit
     * @param protein Updated protein
     * @param fat Updated fat
     * @param carbs Updated carbs
     * @return The updated food entity
     * @throws IllegalArgumentException if food is not user-created
     */
    suspend fun updateCustomFood(
        foodId: Long,
        name: String,
        servingSizeValue: Double,
        servingUnit: ServingUnit,
        protein: Double,
        fat: Double,
        carbs: Double
    ): FoodEntity

    /**
     * Updates an existing recipe.
     * US-021: Edit Custom Foods and Recipes
     *
     * @param recipeId ID of the recipe to update
     * @param name Updated name
     * @param numberOfServings Updated number of servings
     * @param ingredients Updated list of ingredients
     * @return The updated recipe entity
     * @throws IllegalArgumentException if food is not a recipe
     */
    suspend fun updateRecipe(
        recipeId: Long,
        name: String,
        numberOfServings: Double,
        ingredients: List<RecipeIngredientInput>
    ): FoodEntity

    /**
     * Deletes a custom food or recipe.
     * US-022: Delete Custom Foods and Recipes
     *
     * @param foodId ID of the food to delete
     * @throws IllegalArgumentException if food is not user-created
     */
    suspend fun deleteCustomFood(foodId: Long)

    /**
     * Gets all user-created foods (simple foods and recipes).
     * US-020: View and Use Custom Foods in Food Search
     */
    fun getAllUserCreatedFoods(): Flow<List<FoodEntity>>

    /**
     * Searches all foods (including custom) with the given query.
     * US-020: AC-015-001 - Custom foods appear in search with badges
     */
    fun searchAllFoods(query: String): Flow<List<FoodEntity>>

    /**
     * Searches foods excluding recipes (for ingredient selection).
     * EC-014-001: Recipes cannot contain other recipes
     */
    fun searchFoodsForIngredients(query: String): Flow<List<FoodEntity>>

    /**
     * Gets all foods excluding recipes (for ingredient selection).
     */
    fun getAllFoodsForIngredients(): Flow<List<FoodEntity>>

    /**
     * Gets a recipe with all its ingredients.
     * Used for editing recipes.
     */
    suspend fun getRecipeWithIngredients(recipeId: Long): RecipeWithIngredients?

    /**
     * Checks if a food name already exists (case-insensitive).
     * EC-013-007: Warning for duplicate names
     */
    suspend fun foodNameExists(name: String): Boolean

    /**
     * Gets the count of recipes that use a food as an ingredient.
     * EC-016-003, EC-017-003: Warning when food is used in recipes
     */
    suspend fun getRecipeUsageCount(foodId: Long): Int

    /**
     * Gets the count of intake entries that use a food.
     * EC-016-002: Warning when food is in logs
     */
    suspend fun getIntakeUsageCount(foodId: Long): Int
}

/**
 * Input data for a recipe ingredient.
 * Used when creating or updating recipes.
 */
data class RecipeIngredientInput(
    val foodId: Long,
    val quantity: Double,
    val unit: ServingUnit,
    val sortOrder: Int = 0
)
