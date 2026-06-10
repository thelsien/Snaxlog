package com.snaxlog.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for FoodEntity.
 * EPIC-006: User-Created Foods and Recipes
 */
class FoodEntityTest {

    // ============================================================
    // Calorie Calculation Tests
    // ============================================================

    @Test
    fun `calculateCalories with standard macros`() {
        // Protein: 10g * 4 = 40 cal
        // Fat: 5g * 9 = 45 cal
        // Carbs: 20g * 4 = 80 cal
        // Total = 165 cal
        val result = FoodEntity.calculateCalories(
            protein = 10.0,
            fat = 5.0,
            carbs = 20.0
        )
        assertEquals(165, result)
    }

    @Test
    fun `calculateCalories with zero values`() {
        val result = FoodEntity.calculateCalories(
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )
        assertEquals(0, result)
    }

    @Test
    fun `calculateCalories with only protein`() {
        // Protein: 25g * 4 = 100 cal
        val result = FoodEntity.calculateCalories(
            protein = 25.0,
            fat = 0.0,
            carbs = 0.0
        )
        assertEquals(100, result)
    }

    @Test
    fun `calculateCalories with only fat`() {
        // Fat: 10g * 9 = 90 cal
        val result = FoodEntity.calculateCalories(
            protein = 0.0,
            fat = 10.0,
            carbs = 0.0
        )
        assertEquals(90, result)
    }

    @Test
    fun `calculateCalories with only carbs`() {
        // Carbs: 50g * 4 = 200 cal
        val result = FoodEntity.calculateCalories(
            protein = 0.0,
            fat = 0.0,
            carbs = 50.0
        )
        assertEquals(200, result)
    }

    @Test
    fun `calculateCalories with decimal values`() {
        // Protein: 10.5g * 4 = 42 cal
        // Fat: 5.5g * 9 = 49.5 cal
        // Carbs: 20.5g * 4 = 82 cal
        // Total = 173.5, rounded to 174
        val result = FoodEntity.calculateCalories(
            protein = 10.5,
            fat = 5.5,
            carbs = 20.5
        )
        assertEquals(174, result)
    }

    @Test
    fun `calculateCalories rounds fractional calories to nearest integer`() {
        // Protein: 1g * 4 = 4 cal
        // Fat: 0.1g * 9 = 0.9 cal
        // Carbs: 0g * 4 = 0 cal
        // Total = 4.9, rounded to 5
        val result = FoodEntity.calculateCalories(
            protein = 1.0,
            fat = 0.1,
            carbs = 0.0
        )
        assertEquals(5, result)
    }

    @Test
    fun `calculateCalories with large values`() {
        // Protein: 100g * 4 = 400 cal
        // Fat: 100g * 9 = 900 cal
        // Carbs: 100g * 4 = 400 cal
        // Total = 1700 cal
        val result = FoodEntity.calculateCalories(
            protein = 100.0,
            fat = 100.0,
            carbs = 100.0
        )
        assertEquals(1700, result)
    }

    // ============================================================
    // FoodType Property Tests
    // ============================================================

    @Test
    fun `isEditable returns true for user-created foods`() {
        val customFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertTrue(customFood.isEditable)
    }

    @Test
    fun `isEditable returns false for pre-loaded foods`() {
        val predefinedFood = createFood(isUserCreated = false, foodType = FoodType.PREDEFINED)
        assertFalse(predefinedFood.isEditable)
    }

    @Test
    fun `isDeletable returns true for user-created foods`() {
        val customFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertTrue(customFood.isDeletable)
    }

    @Test
    fun `isDeletable returns false for pre-loaded foods`() {
        val predefinedFood = createFood(isUserCreated = false, foodType = FoodType.PREDEFINED)
        assertFalse(predefinedFood.isDeletable)
    }

    @Test
    fun `isRecipe returns true for recipe type`() {
        val recipe = createFood(isUserCreated = true, foodType = FoodType.RECIPE)
        assertTrue(recipe.isRecipe)
    }

    @Test
    fun `isRecipe returns false for simple type`() {
        val simpleFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertFalse(simpleFood.isRecipe)
    }

    @Test
    fun `isSimpleCustomFood returns true for simple type`() {
        val simpleFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertTrue(simpleFood.isSimpleCustomFood)
    }

    @Test
    fun `isSimpleCustomFood returns false for recipe type`() {
        val recipe = createFood(isUserCreated = true, foodType = FoodType.RECIPE)
        assertFalse(recipe.isSimpleCustomFood)
    }

    @Test
    fun `isPredefined returns true for predefined type`() {
        val predefinedFood = createFood(isUserCreated = false, foodType = FoodType.PREDEFINED)
        assertTrue(predefinedFood.isPredefined)
    }

    @Test
    fun `isPredefined returns false for custom food`() {
        val customFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertFalse(customFood.isPredefined)
    }

    @Test
    fun `canBeIngredient returns true for simple food`() {
        val simpleFood = createFood(isUserCreated = true, foodType = FoodType.SIMPLE)
        assertTrue(simpleFood.canBeIngredient)
    }

    @Test
    fun `canBeIngredient returns true for predefined food`() {
        val predefinedFood = createFood(isUserCreated = false, foodType = FoodType.PREDEFINED)
        assertTrue(predefinedFood.canBeIngredient)
    }

    @Test
    fun `canBeIngredient returns false for recipe`() {
        val recipe = createFood(isUserCreated = true, foodType = FoodType.RECIPE)
        assertFalse(recipe.canBeIngredient)
    }

    // ============================================================
    // ServingUnit Tests
    // ============================================================

    @Test
    fun `ServingUnit displayName returns correct values`() {
        assertEquals("grams", ServingUnit.GRAM.displayName)
        assertEquals("ounces", ServingUnit.OUNCE.displayName)
        assertEquals("milliliters", ServingUnit.MILLILITER.displayName)
        assertEquals("cups", ServingUnit.CUP.displayName)
        assertEquals("tablespoons", ServingUnit.TABLESPOON.displayName)
        assertEquals("teaspoons", ServingUnit.TEASPOON.displayName)
        assertEquals("pieces", ServingUnit.PIECE.displayName)
        assertEquals("servings", ServingUnit.SERVING.displayName)
    }

    @Test
    fun `ServingUnit abbreviation returns correct values`() {
        assertEquals("g", ServingUnit.GRAM.abbreviation)
        assertEquals("oz", ServingUnit.OUNCE.abbreviation)
        assertEquals("ml", ServingUnit.MILLILITER.abbreviation)
        assertEquals("cup", ServingUnit.CUP.abbreviation)
        assertEquals("tbsp", ServingUnit.TABLESPOON.abbreviation)
        assertEquals("tsp", ServingUnit.TEASPOON.abbreviation)
        assertEquals("piece", ServingUnit.PIECE.abbreviation)
        assertEquals("serving", ServingUnit.SERVING.abbreviation)
    }

    @Test
    fun `ServingUnit fromString parses name correctly`() {
        assertEquals(ServingUnit.GRAM, ServingUnit.fromString("GRAM"))
        assertEquals(ServingUnit.CUP, ServingUnit.fromString("CUP"))
        assertEquals(ServingUnit.SERVING, ServingUnit.fromString("SERVING"))
    }

    @Test
    fun `ServingUnit fromString parses abbreviation correctly`() {
        assertEquals(ServingUnit.GRAM, ServingUnit.fromString("g"))
        assertEquals(ServingUnit.CUP, ServingUnit.fromString("cup"))
        assertEquals(ServingUnit.TABLESPOON, ServingUnit.fromString("tbsp"))
    }

    @Test
    fun `ServingUnit fromString returns SERVING for unknown value`() {
        assertEquals(ServingUnit.SERVING, ServingUnit.fromString("unknown"))
        assertEquals(ServingUnit.SERVING, ServingUnit.fromString(""))
        assertEquals(ServingUnit.SERVING, ServingUnit.fromString("xyz"))
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    private fun createFood(
        isUserCreated: Boolean,
        foodType: FoodType
    ): FoodEntity {
        return FoodEntity(
            id = 1,
            name = "Test Food",
            category = "Test",
            servingSize = "1 serving",
            servingWeightGrams = 100.0,
            caloriesPerServing = 100,
            proteinPerServing = 10.0,
            fatPerServing = 5.0,
            carbsPerServing = 10.0,
            isUserCreated = isUserCreated,
            foodType = foodType
        )
    }
}
