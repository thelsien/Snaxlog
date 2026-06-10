package com.snaxlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.roundToInt

/**
 * EPIC-006: User-Created Foods and Recipes
 * US-019: Create Recipe with Multiple Ingredients
 *
 * Junction table linking recipes to their ingredient foods with quantities.
 * A recipe (FoodEntity with foodType=RECIPE) can have multiple ingredients.
 *
 * Cascade behavior:
 * - Delete recipe -> CASCADE delete all RecipeIngredient entries
 * - Delete ingredient food -> NO ACTION (recipes must handle missing ingredients gracefully)
 *
 * @property id Unique identifier for the ingredient entry
 * @property recipeId Foreign key to Food table where foodType='RECIPE'
 * @property ingredientFoodId Foreign key to Food table for the ingredient (cannot be a recipe)
 * @property quantity Quantity of ingredient used (must be > 0)
 * @property unit Unit for the quantity (e.g., g, oz, ml, cup, serving)
 * @property sortOrder Display order of ingredients in the recipe (0-based)
 */
@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientFoodId"],
            onDelete = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("recipeId"),
        Index("ingredientFoodId")
    ]
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipeId: Long,
    val ingredientFoodId: Long,
    val quantity: Double,
    val unit: ServingUnit,
    val sortOrder: Int = 0
)

/**
 * Represents a recipe ingredient with its associated food details.
 * Used for displaying ingredients in the UI with full food information.
 */
data class RecipeIngredientWithFood(
    val ingredient: RecipeIngredientEntity,
    val food: FoodEntity?
) {
    /**
     * Returns true if the ingredient food has been deleted.
     * EC-014-010: Handle missing ingredients gracefully.
     */
    fun isMissing(): Boolean = food == null

    /**
     * Calculates the nutritional contribution of this ingredient.
     * Returns null if the food is missing.
     *
     * @return Nutrition contribution or null if food is missing
     */
    fun calculateNutrition(): IngredientNutrition? {
        val f = food ?: return null

        // Calculate how many "servings" worth of the ingredient we're using
        // The quantity is in the ingredient's unit, food has per-serving values
        val servingMultiplier = ingredient.quantity

        return IngredientNutrition(
            calories = (f.caloriesPerServing * servingMultiplier).roundToInt(),
            protein = f.proteinPerServing * servingMultiplier,
            fat = f.fatPerServing * servingMultiplier,
            carbs = f.carbsPerServing * servingMultiplier
        )
    }
}

/**
 * Nutritional values for an ingredient.
 */
data class IngredientNutrition(
    val calories: Int,
    val protein: Double,
    val fat: Double,
    val carbs: Double
)

/**
 * Represents a complete recipe with all its ingredients.
 * Used for recipe creation, editing, and display.
 */
data class RecipeWithIngredients(
    val recipe: FoodEntity,
    val ingredients: List<RecipeIngredientWithFood>
) {
    /**
     * Returns true if any ingredients are missing (deleted).
     */
    fun hasMissingIngredients(): Boolean = ingredients.any { it.isMissing() }

    /**
     * Returns the list of missing ingredient IDs.
     */
    fun getMissingIngredientIds(): List<Long> = ingredients
        .filter { it.isMissing() }
        .map { it.ingredient.ingredientFoodId }

    /**
     * Calculates the total nutrition for the entire recipe.
     * Excludes missing ingredients from the calculation.
     */
    fun calculateTotalNutrition(): RecipeNutrition {
        var totalCalories = 0
        var totalProtein = 0.0
        var totalFat = 0.0
        var totalCarbs = 0.0

        for (ingredientWithFood in ingredients) {
            val nutrition = ingredientWithFood.calculateNutrition() ?: continue
            totalCalories += nutrition.calories
            totalProtein += nutrition.protein
            totalFat += nutrition.fat
            totalCarbs += nutrition.carbs
        }

        return RecipeNutrition(
            totalCalories = totalCalories,
            totalProtein = totalProtein,
            totalFat = totalFat,
            totalCarbs = totalCarbs,
            numberOfServings = recipe.numberOfServings ?: 1.0
        )
    }
}

/**
 * Complete nutritional values for a recipe including per-serving calculations.
 */
data class RecipeNutrition(
    val totalCalories: Int,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val numberOfServings: Double
) {
    /**
     * Calories per serving, rounded to nearest integer.
     */
    val caloriesPerServing: Int
        get() = if (numberOfServings > 0) (totalCalories / numberOfServings).roundToInt() else 0

    /**
     * Protein per serving (2 decimal places precision).
     */
    val proteinPerServing: Double
        get() = if (numberOfServings > 0) totalProtein / numberOfServings else 0.0

    /**
     * Fat per serving (2 decimal places precision).
     */
    val fatPerServing: Double
        get() = if (numberOfServings > 0) totalFat / numberOfServings else 0.0

    /**
     * Carbs per serving (2 decimal places precision).
     */
    val carbsPerServing: Double
        get() = if (numberOfServings > 0) totalCarbs / numberOfServings else 0.0
}
