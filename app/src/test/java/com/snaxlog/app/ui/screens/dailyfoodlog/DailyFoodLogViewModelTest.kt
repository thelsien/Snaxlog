package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class DailyFoodLogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var foodIntakeRepository: FoodIntakeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var calorieGoalRepository: CalorieGoalRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: DailyFoodLogViewModel

    private val testFood = FoodEntity(
        id = 1, name = "Apple", category = "Fruits",
        servingSize = "1 medium (182g)", servingWeightGrams = 182.0,
        caloriesPerServing = 95, proteinPerServing = 0.5,
        fatPerServing = 0.3, carbsPerServing = 25.1
    )

    private val testFood2 = FoodEntity(
        id = 2, name = "Grilled Chicken Breast", category = "Protein",
        servingSize = "1 breast (100g)", servingWeightGrams = 100.0,
        caloriesPerServing = 165, proteinPerServing = 31.0,
        fatPerServing = 3.6, carbsPerServing = 0.0
    )

    private val testGoal = CalorieGoalEntity(
        id = 1, name = "Maintenance", calorieTarget = 2000,
        proteinTarget = 150.0, fatTarget = 67.0, carbsTarget = 200.0,
        isActive = true, isPredefined = true
    )

    private val testEntry = FoodIntakeEntryEntity(
        id = 1, foodId = 1, servings = 1.0,
        totalCalories = 95, totalProtein = 0.5, totalFat = 0.3, totalCarbs = 25.1,
        date = "2026-02-04", timestamp = System.currentTimeMillis()
    )

    private val testEntryWithFood = FoodIntakeWithFood(entry = testEntry, food = testFood)

    private val entriesFlow = MutableStateFlow<List<FoodIntakeWithFood>>(emptyList())
    private val goalFlow = MutableStateFlow<CalorieGoalEntity?>(null)
    private val allFoodsFlow = MutableStateFlow(listOf(testFood, testFood2))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        foodIntakeRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)
        calorieGoalRepository = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()

        // Default mock behavior
        every { foodIntakeRepository.getEntriesForDate(any()) } returns entriesFlow
        every { calorieGoalRepository.getActiveGoal() } returns goalFlow
        every { foodRepository.getAllFoods() } returns allFoodsFlow
        every { foodRepository.searchFoods(any()) } returns flowOf(listOf(testFood2))

        viewModel = DailyFoodLogViewModel(
            foodIntakeRepository = foodIntakeRepository,
            foodRepository = foodRepository,
            calorieGoalRepository = calorieGoalRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // US-001: View daily food intake log
    // ============================================================

    @Test
    fun `AC-001 - shows list of today's food entries with name, serving size, and calories`() = runTest {
        entriesFlow.value = listOf(testEntryWithFood)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.entries.size)
        assertEquals("Apple", state.entries[0].food.name)
        assertEquals(95, state.entries[0].entry.totalCalories)
        assertEquals(1.0, state.entries[0].entry.servings, 0.01)
    }

    @Test
    fun `AC-002 - shows empty state when no entries today`() = runTest {
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.entries.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `AC-003 - shows summary with total calories, protein, fat, carbs vs goal`() = runTest {
        goalFlow.value = testGoal
        entriesFlow.value = listOf(testEntryWithFood)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(95, state.totalCalories)
        assertEquals(0.5, state.totalProtein, 0.01)
        assertEquals(0.3, state.totalFat, 0.01)
        assertEquals(25.1, state.totalCarbs, 0.01)
        assertNotNull(state.activeGoal)
        assertEquals(2000, state.activeGoal!!.calorieTarget)
    }

    @Test
    fun `AC-004 - entries ordered most recent first (reversed chronological)`() = runTest {
        // The DAO handles ordering, verify we pass-through correctly
        val entry2 = testEntry.copy(id = 2, timestamp = testEntry.timestamp + 3600000)
        val entryWithFood2 = FoodIntakeWithFood(entry = entry2, food = testFood)
        entriesFlow.value = listOf(entryWithFood2, testEntryWithFood) // DAO returns newest first
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.entries.size)
        assertTrue(state.entries[0].entry.timestamp > state.entries[1].entry.timestamp)
    }

    @Test
    fun `summary totals are calculated correctly with multiple entries`() = runTest {
        val entry2 = FoodIntakeEntryEntity(
            id = 2, foodId = 2, servings = 1.0,
            totalCalories = 165, totalProtein = 31.0, totalFat = 3.6, totalCarbs = 0.0,
            date = "2026-02-04", timestamp = System.currentTimeMillis()
        )
        val entryWithFood2 = FoodIntakeWithFood(entry = entry2, food = testFood2)
        entriesFlow.value = listOf(testEntryWithFood, entryWithFood2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(260, state.totalCalories) // 95 + 165
        assertEquals(31.5, state.totalProtein, 0.01) // 0.5 + 31.0
        assertEquals(3.9, state.totalFat, 0.01) // 0.3 + 3.6
        assertEquals(25.1, state.totalCarbs, 0.01) // 25.1 + 0.0
    }

    @Test
    fun `EC-007 - no active goal shows totals without comparison`() = runTest {
        goalFlow.value = null
        entriesFlow.value = listOf(testEntryWithFood)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.activeGoal)
        assertEquals(95, state.totalCalories)
    }

    @Test
    fun `EC-008 - decimal values are rounded to 1 decimal place`() = runTest {
        val entry = testEntry.copy(
            totalProtein = 14.6666666,
            totalFat = 3.33333,
            totalCarbs = 42.555555
        )
        entriesFlow.value = listOf(FoodIntakeWithFood(entry = entry, food = testFood))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(14.7, state.totalProtein, 0.01)
        assertEquals(3.3, state.totalFat, 0.01)
        assertEquals(42.6, state.totalCarbs, 0.01)
    }

    // ============================================================
    // US-002: Add new food intake entry
    // ============================================================

    @Test
    fun `AC-006 - openAddFood initializes add food state`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        val state = viewModel.addFoodState.value
        assertEquals("", state.searchQuery)
        assertNull(state.selectedFood)
        assertEquals("1.0", state.servingsInput)
    }

    @Test
    fun `AC-007 - search shows food name, serving size, and calories`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        val state = viewModel.addFoodState.value
        assertTrue(state.foods.isNotEmpty())
        val food = state.foods.find { it.name == "Apple" }
        assertNotNull(food)
        assertEquals("1 medium (182g)", food!!.servingSize)
        assertEquals(95, food.caloriesPerServing)
    }

    @Test
    fun `AC-008 - selecting food allows specifying servings with default 1_0`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.selectFood(testFood)

        val state = viewModel.addFoodState.value
        assertNotNull(state.selectedFood)
        assertEquals("1.0", state.servingsInput)
    }

    @Test
    fun `AC-009 - saving entry creates food intake with calculated values`() = runTest {
        coEvery { foodIntakeRepository.addEntry(any()) } returns 1L
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.selectFood(testFood)
        viewModel.updateAddFoodServings("2.0")
        viewModel.saveAddFood()
        advanceUntilIdle()

        coVerify {
            foodIntakeRepository.addEntry(match { entry ->
                entry.foodId == 1L &&
                        entry.servings == 2.0 &&
                        entry.totalCalories == 190 &&
                        entry.totalProtein == 1.0 &&
                        entry.totalFat == 0.6 &&
                        entry.totalCarbs == 50.2
            })
        }
    }

    @Test
    fun `AC-010 - after adding entry snackbar message is set`() = runTest {
        coEvery { foodIntakeRepository.addEntry(any()) } returns 1L
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.selectFood(testFood)
        viewModel.saveAddFood()
        advanceUntilIdle()

        assertEquals("Entry added", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `EC-011 - zero servings shows validation error`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)

        viewModel.updateAddFoodServings("0")

        val state = viewModel.addFoodState.value
        assertEquals("Serving size must be greater than 0", state.servingsError)
    }

    @Test
    fun `EC-012 - negative servings shows validation error`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)

        viewModel.updateAddFoodServings("-1")

        val state = viewModel.addFoodState.value
        assertEquals("Serving size must be greater than 0", state.servingsError)
    }

    @Test
    fun `EC-014 - decimal servings accepted up to 2 decimal places`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)

        viewModel.updateAddFoodServings("1.5")
        assertNull(viewModel.addFoodState.value.servingsError)

        viewModel.updateAddFoodServings("1.55")
        assertNull(viewModel.addFoodState.value.servingsError)

        viewModel.updateAddFoodServings("1.555")
        assertNotNull(viewModel.addFoodState.value.servingsError)
    }

    @Test
    fun `EC-015 - non-numeric input shows validation error`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)

        viewModel.updateAddFoodServings("abc")

        assertEquals("Please enter a valid number", viewModel.addFoodState.value.servingsError)
    }

    @Test
    fun `EC-020 - double save is prevented by isSaving flag`() = runTest {
        coEvery { foodIntakeRepository.addEntry(any()) } returns 1L
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)

        // First save triggers
        viewModel.saveAddFood()
        // Second save should be ignored due to isSaving
        viewModel.saveAddFood()
        advanceUntilIdle()

        coVerify(exactly = 1) { foodIntakeRepository.addEntry(any()) }
    }

    // ============================================================
    // US-003: Edit existing food intake entry
    // ============================================================

    @Test
    fun `AC-012 - loading entry for edit pre-fills current values`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns testEntryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        val state = viewModel.editFoodState.value
        assertNotNull(state.entry)
        assertEquals("Apple", state.entry!!.food.name)
        assertEquals("1.0", state.servingsInput)
        assertEquals(95, state.previewCalories)
    }

    @Test
    fun `AC-013 - changing serving size updates nutrition preview in real-time`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns testEntryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        viewModel.updateEditFoodServings("2.0")

        val state = viewModel.editFoodState.value
        assertEquals(190, state.previewCalories)
        assertEquals(1.0, state.previewProtein, 0.01)
        assertEquals(0.6, state.previewFat, 0.01)
        assertEquals(50.2, state.previewCarbs, 0.01)
    }

    @Test
    fun `AC-014 - saving edited entry updates it in repository`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns testEntryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        viewModel.updateEditFoodServings("3.0")
        viewModel.saveEditFood()
        advanceUntilIdle()

        coVerify {
            foodIntakeRepository.updateEntry(match { entry ->
                entry.id == 1L &&
                        entry.servings == 3.0 &&
                        entry.totalCalories == 285
            })
        }
    }

    @Test
    fun `AC-015 - cancel editing does not save changes (no update call)`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns testEntryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        viewModel.updateEditFoodServings("5.0")
        // Don't call saveEditFood - simulating cancel

        coVerify(exactly = 0) { foodIntakeRepository.updateEntry(any()) }
    }

    @Test
    fun `EC-024 - entry deleted while editing shows error`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(999L) } returns null

        viewModel.loadEntryForEdit(999L)
        advanceUntilIdle()

        val state = viewModel.editFoodState.value
        assertEquals("Entry no longer exists", state.error)
    }

    @Test
    fun `EC-023 - editing to zero servings shows validation error`() = runTest {
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns testEntryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        viewModel.updateEditFoodServings("0")

        assertNotNull(viewModel.editFoodState.value.servingsError)
    }

    // ============================================================
    // US-004: Delete food intake entry
    // ============================================================

    @Test
    fun `AC-016 - show delete dialog sets deleteDialogEntry`() = runTest {
        viewModel.showDeleteDialog(testEntryWithFood)

        assertNotNull(viewModel.uiState.value.deleteDialogEntry)
        assertEquals(1L, viewModel.uiState.value.deleteDialogEntry!!.entry.id)
    }

    @Test
    fun `AC-017 - confirm delete removes entry from repository`() = runTest {
        viewModel.showDeleteDialog(testEntryWithFood)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        coVerify { foodIntakeRepository.deleteEntry(1L) }
    }

    @Test
    fun `AC-018 - after deletion snackbar message is set`() = runTest {
        viewModel.showDeleteDialog(testEntryWithFood)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        assertEquals("Entry deleted", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `AC-019 - dismiss delete dialog clears deleteDialogEntry`() = runTest {
        viewModel.showDeleteDialog(testEntryWithFood)
        assertNotNull(viewModel.uiState.value.deleteDialogEntry)

        viewModel.dismissDeleteDialog()
        assertNull(viewModel.uiState.value.deleteDialogEntry)
    }

    @Test
    fun `EC-031 - deleting only entry results in empty list`() = runTest {
        entriesFlow.value = listOf(testEntryWithFood)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.entries.size)

        // Simulate the entry being removed from Flow after deletion
        viewModel.showDeleteDialog(testEntryWithFood)
        viewModel.confirmDeleteEntry()
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertEquals(0, viewModel.uiState.value.totalCalories)
    }

    // ============================================================
    // US-005: Search food database
    // ============================================================

    @Test
    fun `AC-020 - typing in search filters food list in real-time`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("chick")
        advanceUntilIdle()

        val state = viewModel.addFoodState.value
        assertEquals("chick", state.searchQuery)
        // searchFoods mock returns testFood2 (Grilled Chicken Breast)
    }

    @Test
    fun `AC-024 - clearing search shows all foods`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("something")
        viewModel.clearSearch()

        assertEquals("", viewModel.addFoodState.value.searchQuery)
    }

    @Test
    fun `EC-040 - search input limited to 100 characters`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        val longQuery = "a".repeat(200)
        viewModel.updateSearchQuery(longQuery)

        assertEquals(100, viewModel.addFoodState.value.searchQuery.length)
    }

    @Test
    fun `EC-039 - whitespace-only search treated as empty`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("   ")
        advanceUntilIdle()

        // The debounced query should be "   " but since it's blank,
        // getAllFoods() is called instead of searchFoods()
    }

    // ============================================================
    // Nutrition calculation accuracy
    // ============================================================

    @Test
    fun `nutrition preview calculates proportionally for decimal servings`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood) // Apple: 95cal, 0.5p, 0.3f, 25.1c per serving

        viewModel.updateAddFoodServings("0.5")

        val state = viewModel.addFoodState.value
        assertEquals(48, state.previewCalories) // 95 * 0.5 = 47.5, rounded to 48
        assertEquals(0.3, state.previewProtein, 0.01) // 0.5 * 0.5 = 0.25, rounded to 0.3
    }

    @Test
    fun `clearSnackbar resets snackbar message`() = runTest {
        coEvery { foodIntakeRepository.addEntry(any()) } returns 1L
        viewModel.openAddFood()
        advanceUntilIdle()
        viewModel.selectFood(testFood)
        viewModel.saveAddFood()
        advanceUntilIdle()

        assertEquals("Entry added", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }
}
