package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.dao.RecipeIngredientDao
import com.snaxlog.app.data.local.database.TransactionRunner
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.RecipeIngredientEntity
import com.snaxlog.app.data.local.entity.ServingUnit
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock

class FoodRepositoryImplTest {

    /**
     * Pass-through [TransactionRunner] that tracks whether a transaction
     * is active so tests can assert writes happen inside one.
     */
    private class FakeTransactionRunner : TransactionRunner {
        var inTransaction = false
            private set
        var transactionCount = 0
            private set

        override suspend fun <T> invoke(block: suspend () -> T): T {
            transactionCount++
            inTransaction = true
            try {
                return block()
            } finally {
                inTransaction = false
            }
        }
    }

    private lateinit var foodDao: FoodDao
    private lateinit var recipeIngredientDao: RecipeIngredientDao
    private lateinit var transactionRunner: FakeTransactionRunner
    private lateinit var repository: FoodRepositoryImpl

    private val testFoods = listOf(
        FoodEntity(
            id = 1, name = "Apple", category = "Fruits",
            servingSize = "1 medium (182g)", servingWeightGrams = 182.0,
            caloriesPerServing = 95, proteinPerServing = 0.5,
            fatPerServing = 0.3, carbsPerServing = 25.1
        ),
        FoodEntity(
            id = 2, name = "Grilled Chicken Breast", category = "Protein",
            servingSize = "1 breast (100g)", servingWeightGrams = 100.0,
            caloriesPerServing = 165, proteinPerServing = 31.0,
            fatPerServing = 3.6, carbsPerServing = 0.0
        ),
        FoodEntity(
            id = 3, name = "White Rice", category = "Grains",
            servingSize = "1 cup cooked (158g)", servingWeightGrams = 158.0,
            caloriesPerServing = 206, proteinPerServing = 4.3,
            fatPerServing = 0.4, carbsPerServing = 44.5
        )
    )

    @Before
    fun setup() {
        foodDao = mockk(relaxed = true)
        recipeIngredientDao = mockk(relaxed = true)
        transactionRunner = FakeTransactionRunner()
        repository = FoodRepositoryImpl(foodDao, recipeIngredientDao, transactionRunner, Clock.systemDefaultZone())
    }

    // ============================================================
    // Original EPIC-003 tests
    // ============================================================

    @Test
    fun `getAllFoods returns all foods from dao`() = runTest {
        every { foodDao.getAllFoods() } returns flowOf(testFoods)

        val result = repository.getAllFoods().first()

        assertEquals(3, result.size)
        assertEquals("Apple", result[0].name)
        assertEquals("Grilled Chicken Breast", result[1].name)
        assertEquals("White Rice", result[2].name)
    }

    @Test
    fun `searchFoods returns matching foods`() = runTest {
        val chickenOnly = testFoods.filter { it.name.contains("Chicken", ignoreCase = true) }
        every { foodDao.searchFoods("chick") } returns flowOf(chickenOnly)

        val result = repository.searchFoods("chick").first()

        assertEquals(1, result.size)
        assertEquals("Grilled Chicken Breast", result[0].name)
    }

    @Test
    fun `searchFoods returns empty list for no matches`() = runTest {
        every { foodDao.searchFoods("pizza") } returns flowOf(emptyList())

        val result = repository.searchFoods("pizza").first()

        assertEquals(0, result.size)
    }

    @Test
    fun `getFoodById returns food when exists`() = runTest {
        coEvery { foodDao.getFoodById(1L) } returns testFoods[0]

        val result = repository.getFoodById(1L)

        assertNotNull(result)
        assertEquals("Apple", result!!.name)
        assertEquals(95, result.caloriesPerServing)
    }

    @Test
    fun `getFoodById returns null when not found`() = runTest {
        coEvery { foodDao.getFoodById(999L) } returns null

        val result = repository.getFoodById(999L)

        assertNull(result)
    }

    // ============================================================
    // EPIC-006: US-018 Create Simple Custom Food
    // ============================================================

    @Test
    fun `US-018 - createCustomFood creates food with correct attributes`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        val result = repository.createCustomFood(
            name = "My Custom Food",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 10.0,
            fat = 5.0,
            carbs = 20.0
        )

        val captured = foodSlot.captured
        assertEquals("My Custom Food", captured.name)
        assertEquals(100.0, captured.servingSizeValue, 0.01)
        assertEquals(ServingUnit.GRAM, captured.servingUnit)
        assertEquals(10.0, captured.proteinPerServing, 0.01)
        assertEquals(5.0, captured.fatPerServing, 0.01)
        assertEquals(20.0, captured.carbsPerServing, 0.01)
        assertTrue(captured.isUserCreated)
        assertEquals(FoodType.SIMPLE, captured.foodType)
        assertEquals("Custom", captured.category)
        assertEquals(100L, result.id)
    }

    @Test
    fun `US-018 - createCustomFood calculates calories correctly`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        // Protein: 10g * 4 = 40 cal
        // Fat: 5g * 9 = 45 cal
        // Carbs: 20g * 4 = 80 cal
        // Total = 165 cal
        repository.createCustomFood(
            name = "Test Food",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 10.0,
            fat = 5.0,
            carbs = 20.0
        )

        assertEquals(165, foodSlot.captured.caloriesPerServing)
    }

    @Test
    fun `US-018 - createCustomFood trims whitespace from name`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        repository.createCustomFood(
            name = "  Trimmed Name  ",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 10.0,
            fat = 5.0,
            carbs = 20.0
        )

        assertEquals("Trimmed Name", foodSlot.captured.name)
    }

    @Test
    fun `US-018 - createCustomFood formats serving size correctly`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        repository.createCustomFood(
            name = "Test",
            servingSizeValue = 1.5,
            servingUnit = ServingUnit.CUP,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )

        assertEquals("1.5 cup", foodSlot.captured.servingSize)
    }

    @Test
    fun `US-018 - createCustomFood formats whole number serving size`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        repository.createCustomFood(
            name = "Test",
            servingSizeValue = 2.0,
            servingUnit = ServingUnit.PIECE,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )

        assertEquals("2 piece", foodSlot.captured.servingSize)
    }

    @Test
    fun `US-018 - createCustomFood sets timestamps`() = runTest {
        val foodSlot = slot<FoodEntity>()
        coEvery { foodDao.insert(capture(foodSlot)) } returns 100L

        repository.createCustomFood(
            name = "Test",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )

        assertNotNull(foodSlot.captured.createdAt)
        assertNotNull(foodSlot.captured.updatedAt)
        assertEquals(foodSlot.captured.createdAt, foodSlot.captured.updatedAt)
    }

    // ============================================================
    // EPIC-006: US-019 Create Recipe
    // ============================================================

    @Test
    fun `US-019 - createRecipe creates recipe with ingredients`() = runTest {
        val foodSlot = slot<FoodEntity>()
        val ingredientsSlot = slot<List<RecipeIngredientEntity>>()

        coEvery { foodDao.insert(capture(foodSlot)) } returns 200L
        coEvery { foodDao.getFoodsByIds(listOf(1L, 2L)) } returns listOf(testFoods[0], testFoods[1])
        coEvery { recipeIngredientDao.insertAll(capture(ingredientsSlot)) } returns listOf(1L, 2L)

        val ingredients = listOf(
            RecipeIngredientInput(foodId = 1L, quantity = 1.0, unit = ServingUnit.SERVING),
            RecipeIngredientInput(foodId = 2L, quantity = 2.0, unit = ServingUnit.SERVING)
        )

        val result = repository.createRecipe(
            name = "Chicken & Apple Salad",
            numberOfServings = 2.0,
            ingredients = ingredients
        )

        // Verify recipe was created
        assertEquals("Chicken & Apple Salad", foodSlot.captured.name)
        assertEquals(FoodType.RECIPE, foodSlot.captured.foodType)
        assertTrue(foodSlot.captured.isUserCreated)
        assertEquals("Recipe", foodSlot.captured.category)
        assertEquals(2.0, foodSlot.captured.numberOfServings!!, 0.01)

        // Verify ingredients were created
        assertEquals(2, ingredientsSlot.captured.size)
        assertEquals(200L, ingredientsSlot.captured[0].recipeId)
        assertEquals(1L, ingredientsSlot.captured[0].ingredientFoodId)
        assertEquals(2L, ingredientsSlot.captured[1].ingredientFoodId)
    }

    @Test
    fun `US-019 - createRecipe calculates per-serving nutrition`() = runTest {
        val foodSlot = slot<FoodEntity>()

        coEvery { foodDao.insert(capture(foodSlot)) } returns 200L
        coEvery { foodDao.getFoodsByIds(listOf(1L, 2L)) } returns listOf(testFoods[0], testFoods[1])
        coEvery { recipeIngredientDao.insertAll(any()) } returns listOf(1L, 2L)

        // Apple: 95 cal, 0.5p, 0.3f, 25.1c (1 serving)
        // Chicken x2: 330 cal, 62p, 7.2f, 0c (2 servings)
        // Total: 425 cal, 62.5p, 7.5f, 25.1c
        // Per serving (2 servings): 212.5 cal rounded to 213, 31.25p, 3.75f, 12.55c
        val ingredients = listOf(
            RecipeIngredientInput(foodId = 1L, quantity = 1.0, unit = ServingUnit.SERVING),
            RecipeIngredientInput(foodId = 2L, quantity = 2.0, unit = ServingUnit.SERVING)
        )

        repository.createRecipe(
            name = "Test Recipe",
            numberOfServings = 2.0,
            ingredients = ingredients
        )

        assertEquals(213, foodSlot.captured.caloriesPerServing)
        assertEquals(31.25, foodSlot.captured.proteinPerServing, 0.01)
        assertEquals(3.75, foodSlot.captured.fatPerServing, 0.01)
        assertEquals(12.55, foodSlot.captured.carbsPerServing, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-019 - createRecipe fails with empty ingredients`() = runTest {
        repository.createRecipe(
            name = "Empty Recipe",
            numberOfServings = 1.0,
            ingredients = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-019 - createRecipe fails with zero servings`() = runTest {
        coEvery { foodDao.getFoodsByIds(any()) } returns listOf(testFoods[0])

        repository.createRecipe(
            name = "Test",
            numberOfServings = 0.0,
            ingredients = listOf(
                RecipeIngredientInput(foodId = 1L, quantity = 1.0, unit = ServingUnit.SERVING)
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `EC-014-001 - createRecipe fails when ingredient is a recipe`() = runTest {
        val recipeIngredient = testFoods[0].copy(foodType = FoodType.RECIPE)
        coEvery { foodDao.getFoodsByIds(listOf(1L)) } returns listOf(recipeIngredient)

        repository.createRecipe(
            name = "Nested Recipe",
            numberOfServings = 1.0,
            ingredients = listOf(
                RecipeIngredientInput(foodId = 1L, quantity = 1.0, unit = ServingUnit.SERVING)
            )
        )
    }

    // ============================================================
    // EPIC-006: US-021 Update Custom Food
    // ============================================================

    @Test
    fun `US-021 - updateCustomFood updates food correctly`() = runTest {
        val existingFood = FoodEntity(
            id = 100, name = "Old Name", category = "Custom",
            servingSize = "100 g", servingWeightGrams = 0.0,
            caloriesPerServing = 100, proteinPerServing = 10.0,
            fatPerServing = 5.0, carbsPerServing = 10.0,
            isUserCreated = true, foodType = FoodType.SIMPLE,
            servingUnit = ServingUnit.GRAM, servingSizeValue = 100.0
        )
        val foodSlot = slot<FoodEntity>()

        coEvery { foodDao.getFoodById(100L) } returns existingFood
        coEvery { foodDao.update(capture(foodSlot)) } returns Unit

        val result = repository.updateCustomFood(
            foodId = 100L,
            name = "New Name",
            servingSizeValue = 150.0,
            servingUnit = ServingUnit.GRAM,
            protein = 15.0,
            fat = 8.0,
            carbs = 12.0
        )

        assertEquals("New Name", foodSlot.captured.name)
        assertEquals(150.0, foodSlot.captured.servingSizeValue, 0.01)
        assertEquals(15.0, foodSlot.captured.proteinPerServing, 0.01)
        assertEquals(8.0, foodSlot.captured.fatPerServing, 0.01)
        assertEquals(12.0, foodSlot.captured.carbsPerServing, 0.01)
        // Calories: 15*4 + 8*9 + 12*4 = 60 + 72 + 48 = 180
        assertEquals(180, foodSlot.captured.caloriesPerServing)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-021 - updateCustomFood fails for pre-loaded food`() = runTest {
        coEvery { foodDao.getFoodById(1L) } returns testFoods[0] // Pre-loaded food

        repository.updateCustomFood(
            foodId = 1L,
            name = "Modified",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-021 - updateCustomFood fails for recipes`() = runTest {
        val recipe = testFoods[0].copy(isUserCreated = true, foodType = FoodType.RECIPE)
        coEvery { foodDao.getFoodById(1L) } returns recipe

        repository.updateCustomFood(
            foodId = 1L,
            name = "Modified",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-021 - updateCustomFood fails when food not found`() = runTest {
        coEvery { foodDao.getFoodById(999L) } returns null

        repository.updateCustomFood(
            foodId = 999L,
            name = "Modified",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 0.0,
            fat = 0.0,
            carbs = 0.0
        )
    }

    // ============================================================
    // Recipe nutrition propagation on ingredient update
    // ============================================================

    private fun simpleCustomFood(
        id: Long,
        calories: Int,
        protein: Double,
        fat: Double,
        carbs: Double
    ) = FoodEntity(
        id = id, name = "Ingredient $id", category = "Custom",
        servingSize = "100 g", servingWeightGrams = 0.0,
        caloriesPerServing = calories, proteinPerServing = protein,
        fatPerServing = fat, carbsPerServing = carbs,
        isUserCreated = true, foodType = FoodType.SIMPLE,
        servingUnit = ServingUnit.GRAM, servingSizeValue = 100.0
    )

    private fun recipeFood(id: Long, numberOfServings: Double) = FoodEntity(
        id = id, name = "Recipe $id", category = "Recipe",
        servingSize = "1 serving", servingWeightGrams = 0.0,
        caloriesPerServing = 0, proteinPerServing = 0.0,
        fatPerServing = 0.0, carbsPerServing = 0.0,
        isUserCreated = true, foodType = FoodType.RECIPE,
        servingUnit = ServingUnit.SERVING, servingSizeValue = 1.0,
        numberOfServings = numberOfServings
    )

    @Test
    fun `updateCustomFood recomputes macros of all recipes using the food`() = runTest {
        // Ingredient 100 (the food being edited) starts at old values.
        val existingFood = simpleCustomFood(100, calories = 100, protein = 10.0, fat = 5.0, carbs = 10.0)
        // A second ingredient, unchanged, shared by both recipes.
        val otherIngredient = simpleCustomFood(101, calories = 50, protein = 2.0, fat = 1.0, carbs = 8.0)

        // Recipe 200: uses food 100 x2 servings + food 101 x1; recipe yields 2 servings.
        val recipe200 = recipeFood(200, numberOfServings = 2.0)
        val recipe200Ingredients = listOf(
            RecipeIngredientEntity(id = 1, recipeId = 200, ingredientFoodId = 100, quantity = 2.0, unit = ServingUnit.SERVING),
            RecipeIngredientEntity(id = 2, recipeId = 200, ingredientFoodId = 101, quantity = 1.0, unit = ServingUnit.SERVING)
        )

        // Recipe 201: uses food 100 x1; recipe yields 1 serving.
        val recipe201 = recipeFood(201, numberOfServings = 1.0)
        val recipe201Ingredients = listOf(
            RecipeIngredientEntity(id = 3, recipeId = 201, ingredientFoodId = 100, quantity = 1.0, unit = ServingUnit.SERVING)
        )

        coEvery { foodDao.getFoodById(100L) } returns existingFood
        coEvery { recipeIngredientDao.getRecipeIdsUsingFood(100L) } returns listOf(200L, 201L)
        coEvery { foodDao.getFoodById(200L) } returns recipe200
        coEvery { foodDao.getFoodById(201L) } returns recipe201
        coEvery { recipeIngredientDao.getIngredientsForRecipeOnce(200L) } returns recipe200Ingredients
        coEvery { recipeIngredientDao.getIngredientsForRecipeOnce(201L) } returns recipe201Ingredients

        // After the food row is updated to NEW values, recompute reads them back.
        // New food 100: protein 20, fat 10, carbs 30 -> calories = 20*4 + 10*9 + 30*4 = 290.
        val updatedFood100 = existingFood.copy(
            caloriesPerServing = 290, proteinPerServing = 20.0, fatPerServing = 10.0, carbsPerServing = 30.0
        )
        coEvery { foodDao.getFoodsByIds(listOf(100L, 101L)) } returns listOf(updatedFood100, otherIngredient)
        coEvery { foodDao.getFoodsByIds(listOf(100L)) } returns listOf(updatedFood100)

        val recipeSlots = mutableListOf<FoodEntity>()
        coEvery { foodDao.update(capture(recipeSlots)) } returns Unit

        repository.updateCustomFood(
            foodId = 100L,
            name = "Edited Ingredient",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 20.0,
            fat = 10.0,
            carbs = 30.0
        )

        // First update is the food itself; remaining are recipes.
        val updatedRecipe200 = recipeSlots.first { it.id == 200L }
        val updatedRecipe201 = recipeSlots.first { it.id == 201L }

        // Recipe 200 totals: food100 x2 -> cal 580, p40, f20, c60 ; food101 x1 -> cal 50, p2, f1, c8
        // total: cal 630, p42, f21, c68 ; per serving (2): cal 315, p21, f10.5, c34
        assertEquals(315, updatedRecipe200.caloriesPerServing)
        assertEquals(21.0, updatedRecipe200.proteinPerServing, 0.001)
        assertEquals(10.5, updatedRecipe200.fatPerServing, 0.001)
        assertEquals(34.0, updatedRecipe200.carbsPerServing, 0.001)

        // Recipe 201 totals: food100 x1 -> cal 290, p20, f10, c30 ; per serving (1): same.
        assertEquals(290, updatedRecipe201.caloriesPerServing)
        assertEquals(20.0, updatedRecipe201.proteinPerServing, 0.001)
        assertEquals(10.0, updatedRecipe201.fatPerServing, 0.001)
        assertEquals(30.0, updatedRecipe201.carbsPerServing, 0.001)
    }

    @Test
    fun `updateCustomFood with no dependent recipes performs no recipe updates`() = runTest {
        val existingFood = simpleCustomFood(100, calories = 100, protein = 10.0, fat = 5.0, carbs = 10.0)

        coEvery { foodDao.getFoodById(100L) } returns existingFood
        coEvery { recipeIngredientDao.getRecipeIdsUsingFood(100L) } returns emptyList()

        val updateSlots = mutableListOf<FoodEntity>()
        coEvery { foodDao.update(capture(updateSlots)) } returns Unit

        repository.updateCustomFood(
            foodId = 100L,
            name = "Edited",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 15.0,
            fat = 8.0,
            carbs = 12.0
        )

        // Only the food itself was updated; no recipe rows touched.
        assertEquals(1, updateSlots.size)
        assertEquals(100L, updateSlots[0].id)
        coVerify(exactly = 0) { recipeIngredientDao.getIngredientsForRecipeOnce(any()) }
    }

    @Test
    fun `updateCustomFood updates food and recomputes recipes inside a single transaction`() = runTest {
        val existingFood = simpleCustomFood(100, calories = 100, protein = 10.0, fat = 5.0, carbs = 10.0)
        val recipe200 = recipeFood(200, numberOfServings = 1.0)
        val recipe200Ingredients = listOf(
            RecipeIngredientEntity(id = 1, recipeId = 200, ingredientFoodId = 100, quantity = 1.0, unit = ServingUnit.SERVING)
        )

        val updatedFood100 = existingFood.copy(
            caloriesPerServing = 180, proteinPerServing = 15.0, fatPerServing = 8.0, carbsPerServing = 12.0
        )

        coEvery { foodDao.getFoodById(100L) } returns existingFood
        coEvery { recipeIngredientDao.getRecipeIdsUsingFood(100L) } returns listOf(200L)
        coEvery { foodDao.getFoodById(200L) } returns recipe200
        coEvery { recipeIngredientDao.getIngredientsForRecipeOnce(200L) } returns recipe200Ingredients
        coEvery { foodDao.getFoodsByIds(listOf(100L)) } returns listOf(updatedFood100)

        val updatedInTransaction = mutableListOf<Boolean>()
        coEvery { foodDao.update(any()) } coAnswers {
            updatedInTransaction.add(transactionRunner.inTransaction)
        }

        repository.updateCustomFood(
            foodId = 100L,
            name = "Edited",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 15.0,
            fat = 8.0,
            carbs = 12.0
        )

        // One transaction wrapping both the food update and the recipe recompute.
        assertEquals(1, transactionRunner.transactionCount)
        assertEquals(2, updatedInTransaction.size)
        assertTrue(updatedInTransaction.all { it })
    }

    @Test
    fun `updateCustomFood recomputes recipe excluding a missing ingredient without throwing`() = runTest {
        val existingFood = simpleCustomFood(100, calories = 100, protein = 10.0, fat = 5.0, carbs = 10.0)

        // Recipe 200 uses food 100 (present) and food 999 (deleted/missing).
        val recipe200 = recipeFood(200, numberOfServings = 1.0)
        val recipe200Ingredients = listOf(
            RecipeIngredientEntity(id = 1, recipeId = 200, ingredientFoodId = 100, quantity = 2.0, unit = ServingUnit.SERVING),
            RecipeIngredientEntity(id = 2, recipeId = 200, ingredientFoodId = 999, quantity = 1.0, unit = ServingUnit.SERVING)
        )

        val updatedFood100 = existingFood.copy(
            caloriesPerServing = 200, proteinPerServing = 12.0, fatPerServing = 6.0, carbsPerServing = 14.0
        )

        coEvery { foodDao.getFoodById(100L) } returns existingFood
        coEvery { recipeIngredientDao.getRecipeIdsUsingFood(100L) } returns listOf(200L)
        coEvery { foodDao.getFoodById(200L) } returns recipe200
        coEvery { recipeIngredientDao.getIngredientsForRecipeOnce(200L) } returns recipe200Ingredients
        // Missing food 999 not returned by getFoodsByIds.
        coEvery { foodDao.getFoodsByIds(listOf(100L, 999L)) } returns listOf(updatedFood100)

        val recipeSlots = mutableListOf<FoodEntity>()
        coEvery { foodDao.update(capture(recipeSlots)) } returns Unit

        repository.updateCustomFood(
            foodId = 100L,
            name = "Edited",
            servingSizeValue = 100.0,
            servingUnit = ServingUnit.GRAM,
            protein = 12.0,
            fat = 6.0,
            carbs = 14.0
        )

        val updatedRecipe200 = recipeSlots.first { it.id == 200L }

        // Only food 100 x2 counts (999 excluded): cal 400, p24, f12, c28 ; per serving (1): same.
        assertEquals(400, updatedRecipe200.caloriesPerServing)
        assertEquals(24.0, updatedRecipe200.proteinPerServing, 0.001)
        assertEquals(12.0, updatedRecipe200.fatPerServing, 0.001)
        assertEquals(28.0, updatedRecipe200.carbsPerServing, 0.001)
    }

    @Test
    fun `deleteCustomFood does not recompute or update dependent recipes`() = runTest {
        val customFood = simpleCustomFood(100, calories = 100, protein = 10.0, fat = 5.0, carbs = 10.0)
        coEvery { foodDao.getFoodById(100L) } returns customFood

        repository.deleteCustomFood(100L)

        coVerify { foodDao.deleteById(100L) }
        // Decision 3: dependent recipes are NOT recomputed on delete.
        coVerify(exactly = 0) { recipeIngredientDao.getRecipeIdsUsingFood(any()) }
        coVerify(exactly = 0) { recipeIngredientDao.getIngredientsForRecipeOnce(any()) }
        coVerify(exactly = 0) { foodDao.update(any()) }
    }

    // ============================================================
    // EPIC-006: US-021 Update Recipe
    // ============================================================

    @Test
    fun `US-021 - updateRecipe updates recipe correctly`() = runTest {
        val existingRecipe = FoodEntity(
            id = 200, name = "Old Recipe", category = "Recipe",
            servingSize = "1 serving", servingWeightGrams = 0.0,
            caloriesPerServing = 100, proteinPerServing = 10.0,
            fatPerServing = 5.0, carbsPerServing = 10.0,
            isUserCreated = true, foodType = FoodType.RECIPE,
            servingUnit = ServingUnit.SERVING, servingSizeValue = 1.0,
            numberOfServings = 2.0
        )
        val foodSlot = slot<FoodEntity>()

        coEvery { foodDao.getFoodById(200L) } returns existingRecipe
        coEvery { foodDao.getFoodsByIds(listOf(1L)) } returns listOf(testFoods[0])
        coEvery { foodDao.update(capture(foodSlot)) } returns Unit
        coEvery { recipeIngredientDao.deleteAllForRecipe(200L) } returns Unit
        coEvery { recipeIngredientDao.insertAll(any()) } returns listOf(1L)

        val ingredients = listOf(
            RecipeIngredientInput(foodId = 1L, quantity = 2.0, unit = ServingUnit.SERVING)
        )

        repository.updateRecipe(
            recipeId = 200L,
            name = "New Recipe",
            numberOfServings = 4.0,
            ingredients = ingredients
        )

        assertEquals("New Recipe", foodSlot.captured.name)
        assertEquals(4.0, foodSlot.captured.numberOfServings!!, 0.01)

        // Verify old ingredients were deleted and new ones inserted
        coVerify { recipeIngredientDao.deleteAllForRecipe(200L) }
        coVerify { recipeIngredientDao.insertAll(any()) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-021 - updateRecipe fails for simple foods`() = runTest {
        val simpleFood = testFoods[0].copy(isUserCreated = true, foodType = FoodType.SIMPLE)
        coEvery { foodDao.getFoodById(1L) } returns simpleFood

        repository.updateRecipe(
            recipeId = 1L,
            name = "Modified",
            numberOfServings = 1.0,
            ingredients = listOf(
                RecipeIngredientInput(foodId = 2L, quantity = 1.0, unit = ServingUnit.SERVING)
            )
        )
    }

    // ============================================================
    // EPIC-006: US-022 Delete Custom Food
    // ============================================================

    @Test
    fun `US-022 - deleteCustomFood deletes user-created food`() = runTest {
        val customFood = testFoods[0].copy(id = 100, isUserCreated = true, foodType = FoodType.SIMPLE)
        coEvery { foodDao.getFoodById(100L) } returns customFood

        repository.deleteCustomFood(100L)

        coVerify { foodDao.deleteById(100L) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-022 - deleteCustomFood fails for pre-loaded food`() = runTest {
        coEvery { foodDao.getFoodById(1L) } returns testFoods[0]

        repository.deleteCustomFood(1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `US-022 - deleteCustomFood fails when not found`() = runTest {
        coEvery { foodDao.getFoodById(999L) } returns null

        repository.deleteCustomFood(999L)
    }

    // ============================================================
    // EPIC-006: US-020 Search and View Custom Foods
    // ============================================================

    @Test
    fun `US-020 - getAllUserCreatedFoods returns custom foods`() = runTest {
        val customFoods = listOf(
            testFoods[0].copy(isUserCreated = true, foodType = FoodType.SIMPLE),
            testFoods[1].copy(isUserCreated = true, foodType = FoodType.RECIPE)
        )
        every { foodDao.getAllUserCreatedFoods() } returns flowOf(customFoods)

        val result = repository.getAllUserCreatedFoods().first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.isUserCreated })
    }

    @Test
    fun `US-020 - searchFoodsForIngredients excludes recipes`() = runTest {
        val nonRecipeFoods = testFoods.filter { it.foodType != FoodType.RECIPE }
        every { foodDao.searchFoodsExcludingRecipes("test") } returns flowOf(nonRecipeFoods)

        val result = repository.searchFoodsForIngredients("test").first()

        assertTrue(result.none { it.foodType == FoodType.RECIPE })
    }

    // ============================================================
    // EPIC-006: getRecipeWithIngredients
    // ============================================================

    @Test
    fun `getRecipeWithIngredients returns recipe with ingredients`() = runTest {
        val recipe = testFoods[0].copy(id = 200, isUserCreated = true, foodType = FoodType.RECIPE)
        val ingredientEntities = listOf(
            RecipeIngredientEntity(id = 1, recipeId = 200, ingredientFoodId = 1, quantity = 1.0, unit = ServingUnit.SERVING),
            RecipeIngredientEntity(id = 2, recipeId = 200, ingredientFoodId = 2, quantity = 2.0, unit = ServingUnit.SERVING)
        )

        coEvery { foodDao.getFoodById(200L) } returns recipe
        coEvery { recipeIngredientDao.getIngredientsForRecipeOnce(200L) } returns ingredientEntities
        coEvery { foodDao.getFoodsByIds(listOf(1L, 2L)) } returns listOf(testFoods[0], testFoods[1])

        val result = repository.getRecipeWithIngredients(200L)

        assertNotNull(result)
        assertEquals(recipe, result!!.recipe)
        assertEquals(2, result.ingredients.size)
    }

    @Test
    fun `getRecipeWithIngredients returns null for non-recipe`() = runTest {
        coEvery { foodDao.getFoodById(1L) } returns testFoods[0] // Pre-defined, not a recipe

        val result = repository.getRecipeWithIngredients(1L)

        assertNull(result)
    }

    @Test
    fun `getRecipeWithIngredients returns null when not found`() = runTest {
        coEvery { foodDao.getFoodById(999L) } returns null

        val result = repository.getRecipeWithIngredients(999L)

        assertNull(result)
    }

    // ============================================================
    // EPIC-006: foodNameExists
    // ============================================================

    @Test
    fun `foodNameExists returns true when name exists`() = runTest {
        coEvery { foodDao.countFoodsWithName("Apple") } returns 1

        val result = repository.foodNameExists("Apple")

        assertTrue(result)
    }

    @Test
    fun `foodNameExists returns false when name does not exist`() = runTest {
        coEvery { foodDao.countFoodsWithName("Nonexistent") } returns 0

        val result = repository.foodNameExists("Nonexistent")

        assertEquals(false, result)
    }

    @Test
    fun `foodNameExists trims whitespace`() = runTest {
        coEvery { foodDao.countFoodsWithName("Apple") } returns 1

        repository.foodNameExists("  Apple  ")

        coVerify { foodDao.countFoodsWithName("Apple") }
    }

    // ============================================================
    // EPIC-006: Usage counts
    // ============================================================

    @Test
    fun `getRecipeUsageCount returns count from dao`() = runTest {
        coEvery { recipeIngredientDao.countRecipesUsingFood(1L) } returns 3

        val result = repository.getRecipeUsageCount(1L)

        assertEquals(3, result)
    }

    @Test
    fun `getIntakeUsageCount returns count from dao`() = runTest {
        coEvery { foodDao.countIntakeEntriesForFood(1L) } returns 5

        val result = repository.getIntakeUsageCount(1L)

        assertEquals(5, result)
    }

    // ============================================================
    // EC-014-012: Transaction atomicity
    // ============================================================

    @Test
    fun `createRecipe inserts recipe and ingredients inside a single transaction`() = runTest {
        var recipeInsertedInTransaction = false
        var ingredientsInsertedInTransaction = false

        coEvery { foodDao.getFoodsByIds(listOf(1L)) } returns listOf(testFoods[0])
        coEvery { foodDao.insert(any()) } coAnswers {
            recipeInsertedInTransaction = transactionRunner.inTransaction
            200L
        }
        coEvery { recipeIngredientDao.insertAll(any()) } coAnswers {
            ingredientsInsertedInTransaction = transactionRunner.inTransaction
            listOf(1L)
        }

        repository.createRecipe(
            name = "Atomic Recipe",
            numberOfServings = 1.0,
            ingredients = listOf(
                RecipeIngredientInput(foodId = 1L, quantity = 1.0, unit = ServingUnit.SERVING)
            )
        )

        assertEquals(1, transactionRunner.transactionCount)
        assertTrue(recipeInsertedInTransaction)
        assertTrue(ingredientsInsertedInTransaction)
    }

    @Test
    fun `updateRecipe replaces recipe and ingredients inside a single transaction`() = runTest {
        val existingRecipe = FoodEntity(
            id = 200, name = "Old Recipe", category = "Recipe",
            servingSize = "1 serving", servingWeightGrams = 0.0,
            caloriesPerServing = 100, proteinPerServing = 10.0,
            fatPerServing = 5.0, carbsPerServing = 10.0,
            isUserCreated = true, foodType = FoodType.RECIPE,
            servingUnit = ServingUnit.SERVING, servingSizeValue = 1.0,
            numberOfServings = 2.0
        )
        var updatedInTransaction = false
        var deletedInTransaction = false
        var insertedInTransaction = false

        coEvery { foodDao.getFoodById(200L) } returns existingRecipe
        coEvery { foodDao.getFoodsByIds(listOf(1L)) } returns listOf(testFoods[0])
        coEvery { foodDao.update(any()) } coAnswers {
            updatedInTransaction = transactionRunner.inTransaction
        }
        coEvery { recipeIngredientDao.deleteAllForRecipe(200L) } coAnswers {
            deletedInTransaction = transactionRunner.inTransaction
        }
        coEvery { recipeIngredientDao.insertAll(any()) } coAnswers {
            insertedInTransaction = transactionRunner.inTransaction
            listOf(1L)
        }

        repository.updateRecipe(
            recipeId = 200L,
            name = "New Recipe",
            numberOfServings = 4.0,
            ingredients = listOf(
                RecipeIngredientInput(foodId = 1L, quantity = 2.0, unit = ServingUnit.SERVING)
            )
        )

        assertEquals(1, transactionRunner.transactionCount)
        assertTrue(updatedInTransaction)
        assertTrue(deletedInTransaction)
        assertTrue(insertedInTransaction)
    }
}
