package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.dao.RecipeIngredientDao
import com.snaxlog.app.data.local.database.TransactionRunner
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.RecipeIngredientEntity
import com.snaxlog.app.data.local.entity.RecipeIngredientWithFood
import com.snaxlog.app.data.local.entity.RecipeWithIngredients
import com.snaxlog.app.data.local.entity.ServingUnit
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Implementation of FoodRepository.
 * EPIC-003: Pre-loaded Food Database
 * EPIC-006: User-Created Foods and Recipes
 */
@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodDao: FoodDao,
    private val recipeIngredientDao: RecipeIngredientDao,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock
) : FoodRepository {

    // ============================
    // Original EPIC-003 operations
    // ============================

    override fun getAllFoods(): Flow<List<FoodEntity>> =
        foodDao.getAllFoods()

    override fun searchFoods(query: String): Flow<List<FoodEntity>> =
        foodDao.searchFoods(query)

    override suspend fun getFoodById(id: Long): FoodEntity? =
        foodDao.getFoodById(id)

    override fun getDistinctCategories(): Flow<List<String>> =
        foodDao.getDistinctCategories()

    override fun getFoodsByCategory(category: String): Flow<List<FoodEntity>> =
        foodDao.getFoodsByCategory(category)

    // ============================
    // EPIC-006: Custom Food operations
    // ============================

    /**
     * Creates a simple custom food.
     * US-018: Create Simple Custom Food
     */
    override suspend fun createCustomFood(
        name: String,
        servingSizeValue: Double,
        servingUnit: ServingUnit,
        protein: Double,
        fat: Double,
        carbs: Double
    ): FoodEntity {
        val calories = FoodEntity.calculateCalories(protein, fat, carbs)
        val now = clock.millis()

        val food = FoodEntity(
            name = name.trim(),
            category = "Custom",
            servingSize = formatServingSize(servingSizeValue, servingUnit),
            servingWeightGrams = 0.0, // Not applicable for custom foods
            caloriesPerServing = calories,
            proteinPerServing = protein,
            fatPerServing = fat,
            carbsPerServing = carbs,
            isUserCreated = true,
            foodType = FoodType.SIMPLE,
            servingUnit = servingUnit,
            servingSizeValue = servingSizeValue,
            numberOfServings = null,
            createdAt = now,
            updatedAt = now
        )

        val id = foodDao.insert(food)
        return food.copy(id = id)
    }

    /**
     * Creates a recipe with multiple ingredients.
     * US-019: Create Recipe with Multiple Ingredients
     * EC-014-012: Uses transaction for atomic save
     */
    override suspend fun createRecipe(
        name: String,
        numberOfServings: Double,
        ingredients: List<RecipeIngredientInput>
    ): FoodEntity {
        require(ingredients.isNotEmpty()) { "Recipe must have at least one ingredient" }
        require(numberOfServings > 0) { "Number of servings must be greater than 0" }

        // Calculate total nutrition from ingredients
        val nutrition = calculateRecipeNutrition(ingredients)
        val now = clock.millis()

        // Calculate per-serving values
        val caloriesPerServing = (nutrition.totalCalories / numberOfServings).roundToInt()
        val proteinPerServing = nutrition.totalProtein / numberOfServings
        val fatPerServing = nutrition.totalFat / numberOfServings
        val carbsPerServing = nutrition.totalCarbs / numberOfServings

        val recipe = FoodEntity(
            name = name.trim(),
            category = "Recipe",
            servingSize = "1 serving",
            servingWeightGrams = 0.0,
            caloriesPerServing = caloriesPerServing,
            proteinPerServing = proteinPerServing,
            fatPerServing = fatPerServing,
            carbsPerServing = carbsPerServing,
            isUserCreated = true,
            foodType = FoodType.RECIPE,
            servingUnit = ServingUnit.SERVING,
            servingSizeValue = 1.0,
            numberOfServings = numberOfServings,
            createdAt = now,
            updatedAt = now
        )

        // Insert recipe and its ingredients atomically
        val recipeId = transactionRunner {
            val recipeId = foodDao.insert(recipe)
            val ingredientEntities = ingredients.mapIndexed { index, input ->
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    ingredientFoodId = input.foodId,
                    quantity = input.quantity,
                    unit = input.unit,
                    sortOrder = index
                )
            }
            recipeIngredientDao.insertAll(ingredientEntities)
            recipeId
        }

        return recipe.copy(id = recipeId)
    }

    /**
     * Updates an existing custom food.
     * US-021: Edit Custom Foods and Recipes
     */
    override suspend fun updateCustomFood(
        foodId: Long,
        name: String,
        servingSizeValue: Double,
        servingUnit: ServingUnit,
        protein: Double,
        fat: Double,
        carbs: Double
    ): FoodEntity {
        val existing = foodDao.getFoodById(foodId)
            ?: throw IllegalArgumentException("Food not found: $foodId")

        require(existing.isUserCreated) { "Cannot edit pre-loaded foods" }
        require(existing.foodType == FoodType.SIMPLE) { "Use updateRecipe for recipes" }

        val calories = FoodEntity.calculateCalories(protein, fat, carbs)
        val now = clock.millis()

        val updated = existing.copy(
            name = name.trim(),
            servingSize = formatServingSize(servingSizeValue, servingUnit),
            caloriesPerServing = calories,
            proteinPerServing = protein,
            fatPerServing = fat,
            carbsPerServing = carbs,
            servingUnit = servingUnit,
            servingSizeValue = servingSizeValue,
            updatedAt = now
        )

        foodDao.update(updated)
        return updated
    }

    /**
     * Updates an existing recipe.
     * US-021: Edit Custom Foods and Recipes
     */
    override suspend fun updateRecipe(
        recipeId: Long,
        name: String,
        numberOfServings: Double,
        ingredients: List<RecipeIngredientInput>
    ): FoodEntity {
        val existing = foodDao.getFoodById(recipeId)
            ?: throw IllegalArgumentException("Recipe not found: $recipeId")

        require(existing.isUserCreated) { "Cannot edit pre-loaded foods" }
        require(existing.foodType == FoodType.RECIPE) { "Use updateCustomFood for simple foods" }
        require(ingredients.isNotEmpty()) { "Recipe must have at least one ingredient" }
        require(numberOfServings > 0) { "Number of servings must be greater than 0" }

        // Calculate nutrition from ingredients
        val nutrition = calculateRecipeNutrition(ingredients)
        val now = clock.millis()

        // Calculate per-serving values
        val caloriesPerServing = (nutrition.totalCalories / numberOfServings).roundToInt()
        val proteinPerServing = nutrition.totalProtein / numberOfServings
        val fatPerServing = nutrition.totalFat / numberOfServings
        val carbsPerServing = nutrition.totalCarbs / numberOfServings

        val updated = existing.copy(
            name = name.trim(),
            caloriesPerServing = caloriesPerServing,
            proteinPerServing = proteinPerServing,
            fatPerServing = fatPerServing,
            carbsPerServing = carbsPerServing,
            numberOfServings = numberOfServings,
            updatedAt = now
        )

        // Update recipe and replace ingredients (delete old, insert new) atomically
        transactionRunner {
            foodDao.update(updated)
            recipeIngredientDao.deleteAllForRecipe(recipeId)
            val ingredientEntities = ingredients.mapIndexed { index, input ->
                RecipeIngredientEntity(
                    recipeId = recipeId,
                    ingredientFoodId = input.foodId,
                    quantity = input.quantity,
                    unit = input.unit,
                    sortOrder = index
                )
            }
            recipeIngredientDao.insertAll(ingredientEntities)
        }

        return updated
    }

    /**
     * Deletes a custom food or recipe.
     * US-022: Delete Custom Foods and Recipes
     * Note: Recipe ingredients are cascade deleted automatically.
     */
    override suspend fun deleteCustomFood(foodId: Long) {
        val food = foodDao.getFoodById(foodId)
            ?: throw IllegalArgumentException("Food not found: $foodId")

        require(food.isUserCreated) { "Cannot delete pre-loaded foods" }

        foodDao.deleteById(foodId)
    }

    override fun getAllUserCreatedFoods(): Flow<List<FoodEntity>> =
        foodDao.getAllUserCreatedFoods()

    override fun searchAllFoods(query: String): Flow<List<FoodEntity>> =
        foodDao.searchAllFoods(query)

    override fun searchFoodsForIngredients(query: String): Flow<List<FoodEntity>> =
        foodDao.searchFoodsExcludingRecipes(query)

    override fun getAllFoodsForIngredients(): Flow<List<FoodEntity>> =
        foodDao.getAllFoodsExcludingRecipes()

    /**
     * Gets a recipe with all its ingredients.
     */
    override suspend fun getRecipeWithIngredients(recipeId: Long): RecipeWithIngredients? {
        val recipe = foodDao.getFoodById(recipeId) ?: return null

        if (recipe.foodType != FoodType.RECIPE) {
            return null
        }

        val ingredientEntities = recipeIngredientDao.getIngredientsForRecipeOnce(recipeId)
        val foodIds = ingredientEntities.map { it.ingredientFoodId }
        val foods = foodDao.getFoodsByIds(foodIds).associateBy { it.id }

        val ingredientsWithFood = ingredientEntities.map { ingredient ->
            RecipeIngredientWithFood(
                ingredient = ingredient,
                food = foods[ingredient.ingredientFoodId]
            )
        }

        return RecipeWithIngredients(recipe, ingredientsWithFood)
    }

    override suspend fun foodNameExists(name: String): Boolean =
        foodDao.countFoodsWithName(name.trim()) > 0

    override suspend fun getRecipeUsageCount(foodId: Long): Int =
        recipeIngredientDao.countRecipesUsingFood(foodId)

    override suspend fun getIntakeUsageCount(foodId: Long): Int =
        foodDao.countIntakeEntriesForFood(foodId)

    // ============================
    // Private helpers
    // ============================

    private fun formatServingSize(value: Double, unit: ServingUnit): String {
        val formattedValue = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
        return "$formattedValue ${unit.abbreviation}"
    }

    /**
     * Calculates total nutrition from a list of ingredients.
     */
    private suspend fun calculateRecipeNutrition(ingredients: List<RecipeIngredientInput>): RecipeNutritionTotals {
        val foodIds = ingredients.map { it.foodId }
        val foods = foodDao.getFoodsByIds(foodIds).associateBy { it.id }

        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0

        for (ingredient in ingredients) {
            val food = foods[ingredient.foodId]
                ?: throw IllegalArgumentException("Ingredient food not found: ${ingredient.foodId}")

            // EC-014-001: Recipes cannot contain other recipes
            require(food.foodType != FoodType.RECIPE) {
                "Cannot add a recipe as an ingredient"
            }

            val multiplier = ingredient.quantity
            totalCalories += food.caloriesPerServing * multiplier
            totalProtein += food.proteinPerServing * multiplier
            totalFat += food.fatPerServing * multiplier
            totalCarbs += food.carbsPerServing * multiplier
        }

        return RecipeNutritionTotals(totalCalories, totalProtein, totalFat, totalCarbs)
    }

    private data class RecipeNutritionTotals(
        val totalCalories: Double,
        val totalProtein: Double,
        val totalFat: Double,
        val totalCarbs: Double
    )
}
