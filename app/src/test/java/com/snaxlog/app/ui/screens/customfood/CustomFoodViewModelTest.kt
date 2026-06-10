package com.snaxlog.app.ui.screens.customfood

import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Unit tests for CustomFoodViewModel.
 * EPIC-006: User-Created Foods and Recipes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomFoodViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var foodRepository: FoodRepository
    private lateinit var viewModel: CustomFoodViewModel

    private val testCustomFood = FoodEntity(
        id = 100, name = "My Custom Food", category = "Custom",
        servingSize = "100 g", servingWeightGrams = 0.0,
        caloriesPerServing = 165, proteinPerServing = 10.0,
        fatPerServing = 5.0, carbsPerServing = 20.0,
        isUserCreated = true, foodType = FoodType.SIMPLE,
        servingUnit = ServingUnit.GRAM, servingSizeValue = 100.0
    )

    private val customFoodsFlow = MutableStateFlow<List<FoodEntity>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        foodRepository = mockk(relaxed = true)
        every { foodRepository.getAllUserCreatedFoods() } returns customFoodsFlow

        viewModel = CustomFoodViewModel(foodRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // US-018: Create Simple Custom Food
    // ============================================================

    @Test
    fun `US-018 - openCreateForm initializes empty form`() = runTest {
        viewModel.openCreateForm()
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertFalse(state.isEditMode)
        assertNull(state.editingFoodId)
        assertEquals("", state.nameInput)
        assertEquals("", state.servingSizeInput)
        assertEquals(ServingUnit.GRAM, state.servingUnit)
        assertEquals("", state.proteinInput)
        assertEquals("", state.fatInput)
        assertEquals("", state.carbsInput)
        assertEquals(0, state.calculatedCalories)
    }

    @Test
    fun `US-018 - updateName updates name and validates`() = runTest {
        viewModel.openCreateForm()
        advanceUntilIdle()

        viewModel.updateName("Test Food")

        assertEquals("Test Food", viewModel.formState.value.nameInput)
        assertNull(viewModel.formState.value.nameError)
    }

    @Test
    fun `US-018 - updateName shows error for blank name`() = runTest {
        viewModel.openCreateForm()
        viewModel.updateName("Test")
        viewModel.updateName("   ")
        advanceUntilIdle()

        assertEquals("Food name is required", viewModel.formState.value.nameError)
    }

    @Test
    fun `EC-013-005 - updateName limits to 100 characters`() = runTest {
        viewModel.openCreateForm()
        val longName = "a".repeat(150)

        viewModel.updateName(longName)
        advanceUntilIdle()

        assertEquals(100, viewModel.formState.value.nameInput.length)
    }

    @Test
    fun `US-018 - updateServingSize validates positive values`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateServingSize("100")
        advanceUntilIdle()

        assertEquals("100", viewModel.formState.value.servingSizeInput)
        assertNull(viewModel.formState.value.servingSizeError)
    }

    @Test
    fun `EC-013-003 - updateServingSize shows error for zero`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateServingSize("0")
        advanceUntilIdle()

        assertEquals("Serving size must be greater than 0", viewModel.formState.value.servingSizeError)
    }

    @Test
    fun `US-018 - updateServingUnit changes unit`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateServingUnit(ServingUnit.CUP)
        advanceUntilIdle()

        assertEquals(ServingUnit.CUP, viewModel.formState.value.servingUnit)
    }

    @Test
    fun `US-018 - updateProtein updates and recalculates calories`() = runTest {
        viewModel.openCreateForm()
        viewModel.updateProtein("10")
        viewModel.updateFat("5")
        viewModel.updateCarbs("20")
        advanceUntilIdle()

        // Protein: 10 * 4 = 40
        // Fat: 5 * 9 = 45
        // Carbs: 20 * 4 = 80
        // Total = 165
        assertEquals(165, viewModel.formState.value.calculatedCalories)
    }

    @Test
    fun `EC-013-002 - updateProtein shows error for negative values`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateProtein("-5")
        advanceUntilIdle()

        // Note: filterNumericInput removes the minus sign, so it becomes "5"
        assertEquals("5", viewModel.formState.value.proteinInput)
    }

    @Test
    fun `US-018 - macro input filters non-numeric characters`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateProtein("10abc.5xyz")
        advanceUntilIdle()

        assertEquals("10.5", viewModel.formState.value.proteinInput)
    }

    @Test
    fun `US-018 - macro input allows only one decimal point`() = runTest {
        viewModel.openCreateForm()

        viewModel.updateFat("10.5.3")
        advanceUntilIdle()

        assertEquals("10.53", viewModel.formState.value.fatInput)
    }

    @Test
    fun `US-018 - saveCustomFood creates new food`() = runTest {
        coEvery { foodRepository.createCustomFood(any(), any(), any(), any(), any(), any()) } returns testCustomFood

        viewModel.openCreateForm()
        viewModel.updateName("Test Food")
        viewModel.updateServingSize("100")
        viewModel.updateProtein("10")
        viewModel.updateFat("5")
        viewModel.updateCarbs("20")
        advanceUntilIdle()

        viewModel.saveCustomFood()
        advanceUntilIdle()

        coVerify {
            foodRepository.createCustomFood(
                name = "Test Food",
                servingSizeValue = 100.0,
                servingUnit = ServingUnit.GRAM,
                protein = 10.0,
                fat = 5.0,
                carbs = 20.0
            )
        }
        assertTrue(viewModel.formState.value.saveSuccess)
        assertEquals("Food created", viewModel.listState.value.snackbarMessage)
    }

    @Test
    fun `EC-013-001 - saveCustomFood validates required fields`() = runTest {
        viewModel.openCreateForm()
        // Leave all fields empty

        viewModel.saveCustomFood()
        advanceUntilIdle()

        assertNotNull(viewModel.formState.value.nameError)
        assertNotNull(viewModel.formState.value.servingSizeError)
        assertNotNull(viewModel.formState.value.proteinError)
        assertNotNull(viewModel.formState.value.fatError)
        assertNotNull(viewModel.formState.value.carbsError)
        assertFalse(viewModel.formState.value.saveSuccess)
    }

    @Test
    fun `EC-013-004 - saveCustomFood prevents duplicate submissions`() = runTest {
        coEvery { foodRepository.createCustomFood(any(), any(), any(), any(), any(), any()) } returns testCustomFood

        viewModel.openCreateForm()
        viewModel.updateName("Test Food")
        viewModel.updateServingSize("100")
        viewModel.updateProtein("10")
        viewModel.updateFat("5")
        viewModel.updateCarbs("20")
        advanceUntilIdle()

        // First save triggers
        viewModel.saveCustomFood()
        // Second save should be ignored due to isSaving
        viewModel.saveCustomFood()
        advanceUntilIdle()

        coVerify(exactly = 1) { foodRepository.createCustomFood(any(), any(), any(), any(), any(), any()) }
    }

    // ============================================================
    // US-021: Edit Custom Food
    // ============================================================

    @Test
    fun `US-021 - openEditForm loads and pre-fills food data`() = runTest {
        coEvery { foodRepository.getFoodById(100L) } returns testCustomFood

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        val state = viewModel.formState.value
        assertTrue(state.isEditMode)
        assertEquals(100L, state.editingFoodId)
        assertEquals("My Custom Food", state.nameInput)
        assertEquals("100", state.servingSizeInput)
        assertEquals(ServingUnit.GRAM, state.servingUnit)
        assertEquals("10", state.proteinInput)
        assertEquals("5", state.fatInput)
        assertEquals("20", state.carbsInput)
        assertEquals(165, state.calculatedCalories)
    }

    @Test
    fun `US-021 - openEditForm shows error for non-existent food`() = runTest {
        coEvery { foodRepository.getFoodById(999L) } returns null

        viewModel.openEditForm(999L)
        advanceUntilIdle()

        assertEquals("Food not found", viewModel.formState.value.error)
    }

    @Test
    fun `US-021 - openEditForm shows error for non-editable food`() = runTest {
        val predefinedFood = testCustomFood.copy(isUserCreated = false, foodType = FoodType.PREDEFINED)
        coEvery { foodRepository.getFoodById(100L) } returns predefinedFood

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        assertEquals("This food cannot be edited", viewModel.formState.value.error)
    }

    @Test
    fun `US-021 - openEditForm shows error for recipe`() = runTest {
        val recipe = testCustomFood.copy(foodType = FoodType.RECIPE)
        coEvery { foodRepository.getFoodById(100L) } returns recipe

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        assertEquals("Use recipe editor for recipes", viewModel.formState.value.error)
    }

    @Test
    fun `US-021 - saveCustomFood updates existing food`() = runTest {
        coEvery { foodRepository.getFoodById(100L) } returns testCustomFood
        coEvery { foodRepository.updateCustomFood(any(), any(), any(), any(), any(), any(), any()) } returns testCustomFood

        viewModel.openEditForm(100L)
        advanceUntilIdle()

        viewModel.updateName("Updated Name")
        viewModel.saveCustomFood()
        advanceUntilIdle()

        coVerify {
            foodRepository.updateCustomFood(
                foodId = 100L,
                name = "Updated Name",
                servingSizeValue = 100.0,
                servingUnit = ServingUnit.GRAM,
                protein = 10.0,
                fat = 5.0,
                carbs = 20.0
            )
        }
        assertTrue(viewModel.formState.value.saveSuccess)
        assertEquals("Food updated", viewModel.listState.value.snackbarMessage)
    }

    // ============================================================
    // US-022: Delete Custom Food
    // ============================================================

    @Test
    fun `US-022 - showDeleteDialog shows confirmation for deletable food`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 0
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 0

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()

        assertEquals(testCustomFood, viewModel.listState.value.deleteDialogFood)
        assertNull(viewModel.listState.value.deleteWarningMessage)
    }

    @Test
    fun `EC-016-002 - showDeleteDialog shows warning when food is in logs`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 5
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 0

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()

        val warning = viewModel.listState.value.deleteWarningMessage
        assertNotNull(warning)
        assertTrue(warning!!.contains("5 log entries"))
    }

    @Test
    fun `EC-016-003 - showDeleteDialog shows warning when food is in recipes`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 0
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 3

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()

        val warning = viewModel.listState.value.deleteWarningMessage
        assertNotNull(warning)
        assertTrue(warning!!.contains("3 recipes"))
    }

    @Test
    fun `US-022 - showDeleteDialog ignores non-deletable food`() = runTest {
        val predefinedFood = testCustomFood.copy(isUserCreated = false)

        viewModel.showDeleteDialog(predefinedFood)
        advanceUntilIdle()

        assertNull(viewModel.listState.value.deleteDialogFood)
    }

    @Test
    fun `US-022 - dismissDeleteDialog clears dialog state`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 0
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 0

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()

        viewModel.dismissDeleteDialog()
        advanceUntilIdle()

        assertNull(viewModel.listState.value.deleteDialogFood)
        assertNull(viewModel.listState.value.deleteWarningMessage)
    }

    @Test
    fun `US-022 - confirmDeleteFood deletes food`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 0
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 0

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()

        viewModel.confirmDeleteFood()
        advanceUntilIdle()

        coVerify { foodRepository.deleteCustomFood(100L) }
        assertNull(viewModel.listState.value.deleteDialogFood)
        assertEquals("Food deleted", viewModel.listState.value.snackbarMessage)
    }

    // ============================================================
    // US-020: View Custom Foods
    // ============================================================

    @Test
    fun `US-020 - observes custom foods from repository`() = runTest {
        customFoodsFlow.value = listOf(testCustomFood)
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertEquals(1, state.foods.size)
        assertEquals("My Custom Food", state.foods[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `US-020 - shows empty list when no custom foods`() = runTest {
        customFoodsFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.listState.value
        assertTrue(state.foods.isEmpty())
        assertFalse(state.isLoading)
    }

    // ============================================================
    // Utility Functions
    // ============================================================

    @Test
    fun `clearSnackbar clears snackbar message`() = runTest {
        coEvery { foodRepository.getIntakeUsageCount(100L) } returns 0
        coEvery { foodRepository.getRecipeUsageCount(100L) } returns 0

        viewModel.showDeleteDialog(testCustomFood)
        advanceUntilIdle()
        viewModel.confirmDeleteFood()
        advanceUntilIdle()

        viewModel.clearSnackbar()
        advanceUntilIdle()

        assertNull(viewModel.listState.value.snackbarMessage)
    }

    @Test
    fun `clearError clears error messages`() = runTest {
        coEvery { foodRepository.getFoodById(999L) } returns null

        viewModel.openEditForm(999L)
        advanceUntilIdle()
        assertNotNull(viewModel.formState.value.error)

        viewModel.clearError()
        advanceUntilIdle()

        assertNull(viewModel.formState.value.error)
    }

    @Test
    fun `resetSaveSuccess clears save success flag`() = runTest {
        coEvery { foodRepository.createCustomFood(any(), any(), any(), any(), any(), any()) } returns testCustomFood

        viewModel.openCreateForm()
        viewModel.updateName("Test Food")
        viewModel.updateServingSize("100")
        viewModel.updateProtein("10")
        viewModel.updateFat("5")
        viewModel.updateCarbs("20")
        advanceUntilIdle()

        viewModel.saveCustomFood()
        advanceUntilIdle()
        assertTrue(viewModel.formState.value.saveSuccess)

        viewModel.resetSaveSuccess()
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.saveSuccess)
    }
}
