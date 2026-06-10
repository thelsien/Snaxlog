package com.snaxlog.app.ui.screens.customfood

import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.RecipeIngredientEntity
import com.snaxlog.app.data.local.entity.RecipeIngredientWithFood
import com.snaxlog.app.data.local.entity.RecipeWithIngredients
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RecipeViewModel.
 * EPIC-006: User-Created Foods and Recipes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var foodRepository: FoodRepository
    private lateinit var viewModel: RecipeViewModel

    private val testFood1 = FoodEntity(
        id = 1, name = "Apple", category = "Fruits",
        servingSize = "1 medium", servingWeightGrams = 182.0,
        caloriesPerServing = 95, proteinPerServing = 0.5,
        fatPerServing = 0.3, carbsPerServing = 25.1,
        foodType = FoodType.PREDEFINED
    )

    private val testFood2 = FoodEntity(
        id = 2, name = "Chicken Breast", category = "Protein",
        servingSize = "100g", servingWeightGrams = 100.0,
        caloriesPerServing = 165, proteinPerServing = 31.0,
        fatPerServing = 3.6, carbsPerServing = 0.0,
        foodType = FoodType.PREDEFINED
    )

    private val testRecipe = FoodEntity(
        id = 100, name = "My Recipe", category = "Recipe",
        servingSize = "1 serving", servingWeightGrams = 0.0,
        caloriesPerServing = 130, proteinPerServing = 15.75,
        fatPerServing = 1.95, carbsPerServing = 12.55,
        isUserCreated = true, foodType = FoodType.RECIPE,
        servingUnit = ServingUnit.SERVING, servingSizeValue = 1.0,
        numberOfServings = 2.0
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodRepository = mockk(relaxed = true)
        viewModel = RecipeViewModel(foodRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // US-019: Create Recipe
    // ============================================================

    @Test
    fun `US-019 - openCreateForm initializes empty form`() = runTest {
        viewModel.openCreateForm()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertFalse(state.isEditMode)
        assertNull(state.editingRecipeId)
        assertEquals("", state.nameInput)
        assertEquals("1", state.numberOfServingsInput)
        assertTrue(state.ingredients.isEmpty())
        assertEquals(0, state.totalCalories)
    }

    @Test
    fun `US-019 - updateName updates name and validates`() = runTest {
        viewModel.openCreateForm()
        viewModel.updateName("Test Recipe")
        advanceUntilIdle()

        assertEquals("Test Recipe", viewModel.formState.value.nameInput)
        assertNull(viewModel.formState.value.nameError)
    }

    @Test
    fun `EC-014-006 - updateName limits to 100 characters`() = runTest {
        viewModel.openCreateForm()
        val longName = "a".repeat(150)

        viewModel.updateName(longName)
        advanceUntilIdle()

        assertEquals(100, viewModel.formState.value.nameInput.length)
    }

    @Test
    fun `EC-014-004 - updateNumberOfServings validates positive values`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateNumberOfServings("4")
        advanceUntilIdle()

        assertEquals("4", viewModel.formState.value.numberOfServingsInput)
        assertNull(viewModel.formState.value.numberOfServingsError)
    }

    @Test
    fun `EC-014-004 - updateNumberOfServings shows error for zero`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateNumberOfServings("0")
        advanceUntilIdle()

        assertEquals("Must be greater than 0", viewModel.formState.value.numberOfServingsError)
    }

    @Test
    fun `AC-014-001 - openIngredientPicker loads foods`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1, testFood2))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertTrue(state.showIngredientPicker)
        assertEquals(2, state.availableFoods.size)
    }

    @Test
    fun `AC-014-001 - searchIngredients filters foods`() = runTest {
        every { foodRepository.searchFoodsForIngredients("chick") } returns flowOf(listOf(testFood2))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        viewModel.searchIngredients("chick")
        advanceUntilIdle()

        assertEquals("chick", viewModel.formState.value.ingredientSearchQuery)
    }

    @Test
    fun `AC-014-002 - selectFoodForAdd selects food for quantity input`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertEquals(testFood1, state.selectedFoodForAdd)
        assertEquals("1", state.ingredientQuantityInput)
        assertEquals(testFood1.servingUnit, state.ingredientUnit)
    }

    @Test
    fun `AC-014-002 - confirmAddIngredient adds ingredient to list`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.updateIngredientQuantity("2")
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertEquals(1, state.ingredients.size)
        assertEquals(testFood1.id, state.ingredients[0].foodId)
        assertEquals(2.0, state.ingredients[0].quantity, 0.01)
        assertNull(state.selectedFoodForAdd) // Reset after adding
    }

    @Test
    fun `confirmAddIngredient shows error for invalid quantity`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.updateIngredientQuantity("0")
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        assertEquals("Please enter a valid quantity", viewModel.formState.value.error)
        assertTrue(viewModel.formState.value.ingredients.isEmpty())
    }

    @Test
    fun `AC-014-003 - adding ingredient recalculates nutrition`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1, testFood2))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        // Add 1 apple: 95 cal
        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        assertEquals(95, viewModel.formState.value.totalCalories)

        // Add 2 chicken: 330 cal, total = 425 cal
        viewModel.selectFoodForAdd(testFood2)
        viewModel.updateIngredientQuantity("2")
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        assertEquals(425, viewModel.formState.value.totalCalories)
        assertEquals(62.5, viewModel.formState.value.totalProtein, 0.01)
    }

    @Test
    fun `removeIngredient removes and recalculates`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1, testFood2))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        viewModel.selectFoodForAdd(testFood2)
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        val ingredientId = viewModel.formState.value.ingredients[0].id
        viewModel.removeIngredient(ingredientId)
        advanceUntilIdle()

        assertEquals(1, viewModel.formState.value.ingredients.size)
        assertEquals(165, viewModel.formState.value.totalCalories)
    }

    @Test
    fun `updateIngredientInList updates quantity and recalculates`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        val ingredientId = viewModel.formState.value.ingredients[0].id
        viewModel.updateIngredientInList(ingredientId, 3.0)
        advanceUntilIdle()

        assertEquals(3.0, viewModel.formState.value.ingredients[0].quantity, 0.01)
        assertEquals(285, viewModel.formState.value.totalCalories) // 95 * 3
    }

    @Test
    fun `US-019 - saveRecipe creates recipe with ingredients`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))
        coEvery { foodRepository.createRecipe(any(), any(), any()) } returns testRecipe

        viewModel.openCreateForm()
        viewModel.updateName("Test Recipe")
        viewModel.updateNumberOfServings("2")
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        viewModel.saveRecipe()
        advanceUntilIdle()

        coVerify {
            foodRepository.createRecipe(
                name = "Test Recipe",
                numberOfServings = 2.0,
                ingredients = match { it.size == 1 && it[0].foodId == testFood1.id }
            )
        }
        assertTrue(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun `EC-014-003 - saveRecipe validates at least one ingredient`() = runTest {
        viewModel.openCreateForm()
        viewModel.updateName("Empty Recipe")
        advanceUntilIdle()

        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("Add at least one ingredient", viewModel.formState.value.ingredientsError)
        assertFalse(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun `EC-014-002 - saveRecipe validates required fields`() = runTest {
        viewModel.openCreateForm()
        // Leave name empty
        advanceUntilIdle()

        viewModel.saveRecipe()
        advanceUntilIdle()

        assertEquals("Recipe name is required", viewModel.formState.value.nameError)
        assertFalse(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun `EC-014-005 - saveRecipe prevents duplicate submissions`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))
        coEvery { foodRepository.createRecipe(any(), any(), any()) } returns testRecipe

        viewModel.openCreateForm()
        viewModel.updateName("Test Recipe")
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        advanceUntilIdle()

        viewModel.saveRecipe()
        viewModel.saveRecipe()
        advanceUntilIdle()

        coVerify(exactly = 1) { foodRepository.createRecipe(any(), any(), any()) }
    }

    // ============================================================
    // US-021: Edit Recipe
    // ============================================================

    @Test
    fun `US-021 - openEditForm loads and pre-fills recipe data`() = runTest {
        val ingredients = listOf(
            RecipeIngredientWithFood(
                ingredient = RecipeIngredientEntity(id = 1, recipeId = 100, ingredientFoodId = 1, quantity = 2.0, unit = ServingUnit.SERVING),
                food = testFood1
            )
        )
        val recipeWithIngredients = RecipeWithIngredients(testRecipe, ingredients)
        coEvery { foodRepository.getRecipeWithIngredients(100L) } returns recipeWithIngredients

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertTrue(state.isEditMode)
        assertEquals(100L, state.editingRecipeId)
        assertEquals("My Recipe", state.nameInput)
        assertEquals("2", state.numberOfServingsInput)
        assertEquals(1, state.ingredients.size)
        assertEquals(testFood1.id, state.ingredients[0].foodId)
    }

    @Test
    fun `US-021 - openEditForm shows error for non-existent recipe`() = runTest {
        coEvery { foodRepository.getRecipeWithIngredients(999L) } returns null

        viewModel.openEditForm(999L)
        advanceUntilIdle()

        assertEquals("Recipe not found", viewModel.formState.value.error)
    }

    @Test
    fun `US-021 - openEditForm shows error for non-recipe food`() = runTest {
        val nonRecipe = testFood1.copy(isUserCreated = true, foodType = FoodType.SIMPLE)
        val recipeWithIngredients = RecipeWithIngredients(nonRecipe, emptyList())
        coEvery { foodRepository.getRecipeWithIngredients(1L) } returns recipeWithIngredients

        viewModel.openEditForm(1L)
        advanceUntilIdle()

        assertEquals("This is not a recipe", viewModel.formState.value.error)
    }

    @Test
    fun `US-021 - saveRecipe updates existing recipe`() = runTest {
        val ingredients = listOf(
            RecipeIngredientWithFood(
                ingredient = RecipeIngredientEntity(id = 1, recipeId = 100, ingredientFoodId = 1, quantity = 2.0, unit = ServingUnit.SERVING),
                food = testFood1
            )
        )
        val recipeWithIngredients = RecipeWithIngredients(testRecipe, ingredients)
        coEvery { foodRepository.getRecipeWithIngredients(100L) } returns recipeWithIngredients
        coEvery { foodRepository.updateRecipe(any(), any(), any(), any()) } returns testRecipe

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        viewModel.updateName("Updated Recipe")
        viewModel.saveRecipe()
        advanceUntilIdle()

        coVerify {
            foodRepository.updateRecipe(
                recipeId = 100L,
                name = "Updated Recipe",
                numberOfServings = 2.0,
                ingredients = any()
            )
        }
        assertTrue(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun `US-021 - openEditForm skips missing ingredient foods`() = runTest {
        val ingredients = listOf(
            RecipeIngredientWithFood(
                ingredient = RecipeIngredientEntity(id = 1, recipeId = 100, ingredientFoodId = 1, quantity = 2.0, unit = ServingUnit.SERVING),
                food = testFood1
            ),
            RecipeIngredientWithFood(
                ingredient = RecipeIngredientEntity(id = 2, recipeId = 100, ingredientFoodId = 999, quantity = 1.0, unit = ServingUnit.SERVING),
                food = null // Deleted food
            )
        )
        val recipeWithIngredients = RecipeWithIngredients(testRecipe, ingredients)
        coEvery { foodRepository.getRecipeWithIngredients(100L) } returns recipeWithIngredients

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        // Only the valid ingredient should be loaded
        assertEquals(1, viewModel.formState.value.ingredients.size)
        assertEquals(testFood1.id, viewModel.formState.value.ingredients[0].foodId)
    }

    // ============================================================
    // Ingredient Picker
    // ============================================================

    @Test
    fun `closeIngredientPicker resets picker state`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.closeIngredientPicker()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertFalse(state.showIngredientPicker)
        assertNull(state.selectedFoodForAdd)
        assertEquals("1", state.ingredientQuantityInput)
        assertEquals("", state.ingredientSearchQuery)
    }

    @Test
    fun `clearFoodSelection clears selected food`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))

        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        advanceUntilIdle()
        assertNotNull(viewModel.formState.value.selectedFoodForAdd)

        viewModel.clearFoodSelection()
        advanceUntilIdle()

        assertNull(viewModel.formState.value.selectedFoodForAdd)
        assertEquals("1", viewModel.formState.value.ingredientQuantityInput)
    }

    // ============================================================
    // Utility Functions
    // ============================================================

    @Test
    fun `clearError clears error message`() = runTest {
        viewModel.openCreateForm()
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.updateIngredientQuantity("0")
        viewModel.confirmAddIngredient()
        advanceUntilIdle()
        assertNotNull(viewModel.formState.value.error)

        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.formState.value.error)
    }

    @Test
    fun `resetSaveSuccess clears save success flag`() = runTest {
        every { foodRepository.searchFoodsForIngredients("") } returns flowOf(listOf(testFood1))
        coEvery { foodRepository.createRecipe(any(), any(), any()) } returns testRecipe

        viewModel.openCreateForm()
        viewModel.updateName("Test Recipe")
        viewModel.openIngredientPicker()
        advanceUntilIdle()

        viewModel.selectFoodForAdd(testFood1)
        viewModel.confirmAddIngredient()
        viewModel.saveRecipe()
        advanceUntilIdle()
        assertTrue(viewModel.formState.value.saveSuccess)

        viewModel.resetSaveSuccess()
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.saveSuccess)
    }
}
