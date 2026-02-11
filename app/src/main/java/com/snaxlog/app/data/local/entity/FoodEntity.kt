package com.snaxlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Type of food in the database.
 * EPIC-006: User-Created Foods and Recipes
 */
enum class FoodType {
    /** Pre-loaded food from the database */
    PREDEFINED,
    /** User-created simple custom food */
    SIMPLE,
    /** User-created recipe with multiple ingredients */
    RECIPE
}

/**
 * Serving size units supported for custom foods.
 * EPIC-006: User-Created Foods and Recipes
 */
enum class ServingUnit(val displayName: String, val abbreviation: String) {
    GRAM("grams", "g"),
    OUNCE("ounces", "oz"),
    MILLILITER("milliliters", "ml"),
    CUP("cups", "cup"),
    TABLESPOON("tablespoons", "tbsp"),
    TEASPOON("teaspoons", "tsp"),
    PIECE("pieces", "piece"),
    SERVING("servings", "serving");

    companion object {
        fun fromString(value: String): ServingUnit {
            return entries.find { it.name == value || it.abbreviation == value } ?: SERVING
        }
    }
}

/**
 * Represents a food item in the food database.
 *
 * EPIC-003: Pre-loaded Food Database - Original read-only reference data
 * EPIC-006: User-Created Foods and Recipes - Extended to support:
 *   - Simple custom foods (user-created single food items)
 *   - Recipes (user-created multi-ingredient meals)
 *
 * @property id Unique identifier for the food
 * @property name Display name of the food
 * @property category Food category (for pre-loaded foods)
 * @property servingSize Human-readable serving size description
 * @property servingWeightGrams Weight in grams (for pre-loaded foods, may be 0 for custom foods)
 * @property caloriesPerServing Calories per single serving
 * @property proteinPerServing Protein in grams per single serving
 * @property fatPerServing Fat in grams per single serving
 * @property carbsPerServing Carbohydrates in grams per single serving
 * @property isUserCreated Whether this food was created by the user (EPIC-006)
 * @property foodType Type of food: PREDEFINED, SIMPLE, or RECIPE (EPIC-006)
 * @property servingUnit Unit of measurement for serving size (EPIC-006)
 * @property servingSizeValue Numeric serving size value (EPIC-006)
 * @property numberOfServings For recipes: how many servings the recipe makes (EPIC-006)
 * @property createdAt Timestamp when custom food was created (EPIC-006)
 * @property updatedAt Timestamp when custom food was last updated (EPIC-006)
 */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val servingSize: String,
    val servingWeightGrams: Double,
    val caloriesPerServing: Int,
    val proteinPerServing: Double,
    val fatPerServing: Double,
    val carbsPerServing: Double,
    // EPIC-006: User-Created Foods and Recipes
    val isUserCreated: Boolean = false,
    val foodType: FoodType = FoodType.PREDEFINED,
    val servingUnit: ServingUnit = ServingUnit.GRAM,
    val servingSizeValue: Double = 0.0,
    val numberOfServings: Double? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    /**
     * Returns true if this food can be edited by the user.
     * Only user-created foods can be edited.
     */
    val isEditable: Boolean
        get() = isUserCreated

    /**
     * Returns true if this food can be deleted by the user.
     * Only user-created foods can be deleted.
     */
    val isDeletable: Boolean
        get() = isUserCreated

    /**
     * Returns true if this food is a recipe.
     */
    val isRecipe: Boolean
        get() = foodType == FoodType.RECIPE

    /**
     * Returns true if this food is a simple custom food.
     */
    val isSimpleCustomFood: Boolean
        get() = foodType == FoodType.SIMPLE

    /**
     * Returns true if this food is a pre-loaded food.
     */
    val isPredefined: Boolean
        get() = foodType == FoodType.PREDEFINED

    /**
     * Returns true if this food can be used as an ingredient in a recipe.
     * Recipes cannot be used as ingredients (prevents circular dependencies).
     */
    val canBeIngredient: Boolean
        get() = foodType != FoodType.RECIPE

    companion object {
        /**
         * Calculates calories from macronutrients using the standard formula:
         * Calories = (protein x 4) + (carbs x 4) + (fat x 9)
         *
         * @param protein Protein in grams
         * @param fat Fat in grams
         * @param carbs Carbohydrates in grams
         * @return Calculated calories, rounded to nearest integer
         */
        fun calculateCalories(protein: Double, fat: Double, carbs: Double): Int {
            return ((protein * 4) + (carbs * 4) + (fat * 9)).toInt()
        }
    }
}
