package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for EPIC-004: Visual Progress Indicators (US-012)
 *
 * Covers:
 * - AC-052: Summary card shows total calories consumed vs goal
 * - AC-053: Summary shows total protein, fat, and carbs (with targets if set)
 * - AC-054: Summary updates immediately on add/edit/delete
 * - AC-055: Visual indicator when over goal (error state)
 * - AC-056: No active goal shows totals without comparison
 * - EC-087: Exactly 100% shows completion indicator
 * - EC-088: 0 calories shows 0/goal
 * - EC-089: 200%+ caps progress indicator
 * - EC-090: Goal changed mid-day recalculates immediately
 * - EC-091: Long decimals rounded to 1 decimal place
 * - EC-092: Database query error handling (covered by existing tests)
 * - EC-093: Macro goals with incomplete food data
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VisualProgressIndicatorsTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var foodIntakeRepository: FoodIntakeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var calorieGoalRepository: CalorieGoalRepository
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

    private val goalWithMacros = CalorieGoalEntity(
        id = 1, name = "Maintenance", calorieTarget = 2000,
        proteinTarget = 150.0, fatTarget = 67.0, carbsTarget = 200.0,
        isActive = true, isPredefined = true
    )

    private val goalCaloriesOnly = CalorieGoalEntity(
        id = 2, name = "Simple Goal", calorieTarget = 2000,
        proteinTarget = null, fatTarget = null, carbsTarget = null,
        isActive = true, isPredefined = true
    )

    private val entriesFlow = MutableStateFlow<List<FoodIntakeWithFood>>(emptyList())
    private val goalFlow = MutableStateFlow<CalorieGoalEntity?>(null)
    private val allFoodsFlow = MutableStateFlow(listOf(testFood, testFood2))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        foodIntakeRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)
        calorieGoalRepository = mockk(relaxed = true)

        every { foodIntakeRepository.getEntriesForDate(any()) } returns entriesFlow
        every { calorieGoalRepository.getActiveGoal() } returns goalFlow
        every { foodRepository.getAllFoods() } returns allFoodsFlow
        every { foodRepository.searchFoods(any()) } returns flowOf(listOf(testFood2))

        viewModel = DailyFoodLogViewModel(
            foodIntakeRepository = foodIntakeRepository,
            foodRepository = foodRepository,
            calorieGoalRepository = calorieGoalRepository,
            savedStateHandle = SavedStateHandle()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // computeNutrientProgress - Unit tests for the core logic
    // ============================================================

    @Test
    fun `computeNutrientProgress - no goal returns NO_GOAL level`() {
        val result = viewModel.computeNutrientProgress(500.0, null)

        assertEquals(ProgressLevel.NO_GOAL, result.progressLevel)
        assertNull(result.progress)
        assertNull(result.remaining)
        assertNull(result.goal)
        assertEquals(500.0, result.consumed, 0.01)
    }

    @Test
    fun `computeNutrientProgress - zero goal returns NO_GOAL level`() {
        val result = viewModel.computeNutrientProgress(500.0, 0.0)

        assertEquals(ProgressLevel.NO_GOAL, result.progressLevel)
        assertNull(result.progress)
    }

    @Test
    fun `computeNutrientProgress - negative goal returns NO_GOAL level`() {
        val result = viewModel.computeNutrientProgress(500.0, -100.0)

        assertEquals(ProgressLevel.NO_GOAL, result.progressLevel)
    }

    @Test
    fun `computeNutrientProgress - 0 percent progress returns NORMAL`() {
        val result = viewModel.computeNutrientProgress(0.0, 2000.0)

        assertEquals(ProgressLevel.NORMAL, result.progressLevel)
        assertEquals(0.0f, result.progress!!, 0.001f)
        assertEquals(2000.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 45 percent progress returns NORMAL`() {
        val result = viewModel.computeNutrientProgress(900.0, 2000.0)

        assertEquals(ProgressLevel.NORMAL, result.progressLevel)
        assertEquals(0.45f, result.progress!!, 0.001f)
        assertEquals(1100.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 89 percent progress returns NORMAL`() {
        val result = viewModel.computeNutrientProgress(1780.0, 2000.0)

        assertEquals(ProgressLevel.NORMAL, result.progressLevel)
        assertEquals(0.89f, result.progress!!, 0.001f)
        assertEquals(220.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 90 percent progress returns APPROACHING`() {
        val result = viewModel.computeNutrientProgress(1800.0, 2000.0)

        assertEquals(ProgressLevel.APPROACHING, result.progressLevel)
        assertEquals(0.9f, result.progress!!, 0.001f)
        assertEquals(200.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 95 percent progress returns APPROACHING`() {
        val result = viewModel.computeNutrientProgress(1900.0, 2000.0)

        assertEquals(ProgressLevel.APPROACHING, result.progressLevel)
        assertEquals(0.95f, result.progress!!, 0.001f)
        assertEquals(100.0, result.remaining!!, 0.01)
    }

    @Test
    fun `EC-087 - computeNutrientProgress - exactly 100 percent returns APPROACHING`() {
        val result = viewModel.computeNutrientProgress(2000.0, 2000.0)

        assertEquals(ProgressLevel.APPROACHING, result.progressLevel)
        assertEquals(1.0f, result.progress!!, 0.001f)
        assertEquals(0.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 101 percent returns EXCEEDED`() {
        val result = viewModel.computeNutrientProgress(2020.0, 2000.0)

        assertEquals(ProgressLevel.EXCEEDED, result.progressLevel)
        assertEquals(1.01f, result.progress!!, 0.001f)
        assertEquals(-20.0, result.remaining!!, 0.01)
    }

    @Test
    fun `computeNutrientProgress - 150 percent returns EXCEEDED`() {
        val result = viewModel.computeNutrientProgress(3000.0, 2000.0)

        assertEquals(ProgressLevel.EXCEEDED, result.progressLevel)
        assertEquals(1.5f, result.progress!!, 0.001f)
        assertEquals(-1000.0, result.remaining!!, 0.01)
    }

    @Test
    fun `EC-089 - computeNutrientProgress - 200 percent returns EXCEEDED with actual progress value`() {
        val result = viewModel.computeNutrientProgress(4000.0, 2000.0)

        assertEquals(ProgressLevel.EXCEEDED, result.progressLevel)
        assertEquals(2.0f, result.progress!!, 0.001f)
        assertEquals(-2000.0, result.remaining!!, 0.01)
        // Note: The UI layer (ProgressBar) clamps visual display to 1.5f
    }

    @Test
    fun `computeNutrientProgress - small consumed value with large goal`() {
        val result = viewModel.computeNutrientProgress(10.0, 2000.0)

        assertEquals(ProgressLevel.NORMAL, result.progressLevel)
        assertEquals(0.005f, result.progress!!, 0.001f)
        assertEquals(1990.0, result.remaining!!, 0.01)
    }

    @Test
    fun `EC-091 - computeNutrientProgress - remaining value rounded to 1 decimal`() {
        // 2000 - 1333.3333 = 666.6667, should round to 666.7
        val result = viewModel.computeNutrientProgress(1333.3333, 2000.0)

        assertEquals(666.7, result.remaining!!, 0.01)
    }

    // ============================================================
    // AC-052: Summary card shows total calories consumed vs goal
    // ============================================================

    @Test
    fun `AC-052 - calorie progress state is computed when entries and goal exist`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 1000, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.calorieProgress.progress)
        assertEquals(0.5f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.calorieProgress.progressLevel)
        assertEquals(1000.0, state.calorieProgress.consumed, 0.01)
        assertEquals(2000.0, state.calorieProgress.goal!!, 0.01)
        assertEquals(1000.0, state.calorieProgress.remaining!!, 0.01)
    }

    // ============================================================
    // AC-053: Summary shows macros with targets if set
    // ============================================================

    @Test
    fun `AC-053 - macro progress states computed when macro goals exist`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 500, protein = 30.0, fat = 20.0, carbs = 50.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Protein: 30/150 = 20%
        assertNotNull(state.proteinProgress.progress)
        assertEquals(0.2f, state.proteinProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.proteinProgress.progressLevel)

        // Fat: 20/67 = ~29.85%
        assertNotNull(state.fatProgress.progress)
        assertEquals(0.2985f, state.fatProgress.progress!!, 0.01f)
        assertEquals(ProgressLevel.NORMAL, state.fatProgress.progressLevel)

        // Carbs: 50/200 = 25%
        assertNotNull(state.carbsProgress.progress)
        assertEquals(0.25f, state.carbsProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.carbsProgress.progressLevel)
    }

    @Test
    fun `macro progress is NO_GOAL when goal has no macro targets`() = runTest {
        goalFlow.value = goalCaloriesOnly
        val entry = createEntry(id = 1, calories = 500, protein = 30.0, fat = 20.0, carbs = 50.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Calorie progress should still work
        assertNotNull(state.calorieProgress.progress)
        assertEquals(0.25f, state.calorieProgress.progress!!, 0.001f)

        // Macro progress should be NO_GOAL since no macro targets
        assertEquals(ProgressLevel.NO_GOAL, state.proteinProgress.progressLevel)
        assertNull(state.proteinProgress.progress)

        assertEquals(ProgressLevel.NO_GOAL, state.fatProgress.progressLevel)
        assertNull(state.fatProgress.progress)

        assertEquals(ProgressLevel.NO_GOAL, state.carbsProgress.progressLevel)
        assertNull(state.carbsProgress.progress)
    }

    // ============================================================
    // AC-054: Summary updates immediately on add/edit/delete
    // ============================================================

    @Test
    fun `AC-054 - progress updates immediately when entry is added`() = runTest {
        goalFlow.value = goalWithMacros
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        // Initially 0%
        assertEquals(0.0f, viewModel.uiState.value.calorieProgress.progress!!, 0.001f)

        // Add an entry
        val entry = createEntry(id = 1, calories = 1000, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        // Now 50%
        assertEquals(0.5f, viewModel.uiState.value.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    @Test
    fun `AC-054 - progress updates immediately when entry is deleted`() = runTest {
        goalFlow.value = goalWithMacros
        val entry1 = createEntry(id = 1, calories = 1000, protein = 50.0, fat = 30.0, carbs = 100.0)
        val entry2 = createEntry(id = 2, calories = 900, protein = 45.0, fat = 25.0, carbs = 80.0)
        entriesFlow.value = listOf(entry1, entry2)
        advanceUntilIdle()

        // Initially 95% (1900/2000) - APPROACHING
        assertEquals(0.95f, viewModel.uiState.value.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.APPROACHING, viewModel.uiState.value.calorieProgress.progressLevel)

        // Delete entry2 -> 50% (1000/2000) - NORMAL
        entriesFlow.value = listOf(entry1)
        advanceUntilIdle()

        assertEquals(0.5f, viewModel.uiState.value.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    // ============================================================
    // AC-055: Visual indicator when over goal
    // ============================================================

    @Test
    fun `AC-055 - EXCEEDED progress level when calories exceed goal`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 2500, protein = 180.0, fat = 80.0, carbs = 250.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProgressLevel.EXCEEDED, state.calorieProgress.progressLevel)
        assertEquals(1.25f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(-500.0, state.calorieProgress.remaining!!, 0.01)
    }

    @Test
    fun `AC-055 - macro progress shows EXCEEDED when macro exceeds goal`() = runTest {
        goalFlow.value = goalWithMacros // protein goal = 150
        val entry = createEntry(id = 1, calories = 500, protein = 160.0, fat = 10.0, carbs = 30.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Protein 160/150 = 106.7% -> EXCEEDED
        assertEquals(ProgressLevel.EXCEEDED, state.proteinProgress.progressLevel)
        assertEquals(1.0667f, state.proteinProgress.progress!!, 0.01f)
    }

    // ============================================================
    // AC-056: No active goal shows totals without comparison
    // ============================================================

    @Test
    fun `AC-056 - no goal results in NO_GOAL for all progress states`() = runTest {
        goalFlow.value = null
        val entry = createEntry(id = 1, calories = 1000, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.activeGoal)

        assertEquals(ProgressLevel.NO_GOAL, state.calorieProgress.progressLevel)
        assertNull(state.calorieProgress.progress)
        assertNull(state.calorieProgress.remaining)

        assertEquals(ProgressLevel.NO_GOAL, state.proteinProgress.progressLevel)
        assertEquals(ProgressLevel.NO_GOAL, state.fatProgress.progressLevel)
        assertEquals(ProgressLevel.NO_GOAL, state.carbsProgress.progressLevel)

        // But totals are still computed
        assertEquals(1000, state.totalCalories)
        assertEquals(50.0, state.totalProtein, 0.01)
    }

    // ============================================================
    // EC-087: Exactly 100% shows completion indicator
    // ============================================================

    @Test
    fun `EC-087 - exactly at goal shows APPROACHING level`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        val entry = createEntry(id = 1, calories = 2000, protein = 150.0, fat = 67.0, carbs = 200.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProgressLevel.APPROACHING, state.calorieProgress.progressLevel)
        assertEquals(1.0f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(0.0, state.calorieProgress.remaining!!, 0.01)
    }

    // ============================================================
    // EC-088: 0 calories shows 0/goal
    // ============================================================

    @Test
    fun `EC-088 - zero consumed with goal shows 0 percent progress`() = runTest {
        goalFlow.value = goalWithMacros
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.totalCalories)
        assertEquals(0.0f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.calorieProgress.progressLevel)
        assertEquals(2000.0, state.calorieProgress.remaining!!, 0.01)
    }

    // ============================================================
    // EC-089: 200%+ caps progress indicator
    // ============================================================

    @Test
    fun `EC-089 - 200 percent consumed tracks actual progress value`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        val entry = createEntry(id = 1, calories = 4000, protein = 300.0, fat = 134.0, carbs = 400.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Progress is tracked at full value (2.0 = 200%)
        assertEquals(2.0f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.EXCEEDED, state.calorieProgress.progressLevel)
        assertEquals(-2000.0, state.calorieProgress.remaining!!, 0.01)
        // Note: The ProgressBar composable visually caps at 1.5f (PROGRESS_VISUAL_CAP)
    }

    @Test
    fun `EC-089 - 300 percent consumed still tracked correctly`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 6000, protein = 450.0, fat = 200.0, carbs = 600.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(3.0f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.EXCEEDED, state.calorieProgress.progressLevel)
    }

    // ============================================================
    // EC-090: Goal changed mid-day recalculates immediately
    // ============================================================

    @Test
    fun `EC-090 - changing goal mid-day recalculates all progress`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        val entry = createEntry(id = 1, calories = 1800, protein = 135.0, fat = 60.0, carbs = 180.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        // 1800/2000 = 90% -> APPROACHING
        assertEquals(ProgressLevel.APPROACHING, viewModel.uiState.value.calorieProgress.progressLevel)
        assertEquals(0.9f, viewModel.uiState.value.calorieProgress.progress!!, 0.001f)

        // Change goal to 1500 -> 1800/1500 = 120% -> EXCEEDED
        val newGoal = CalorieGoalEntity(
            id = 3, name = "Cut", calorieTarget = 1500,
            proteinTarget = 120.0, fatTarget = 50.0, carbsTarget = 150.0,
            isActive = true, isPredefined = false
        )
        goalFlow.value = newGoal
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ProgressLevel.EXCEEDED, state.calorieProgress.progressLevel)
        assertEquals(1.2f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(-300.0, state.calorieProgress.remaining!!, 0.01)

        // Protein: 135/120 = 112.5% -> EXCEEDED
        assertEquals(ProgressLevel.EXCEEDED, state.proteinProgress.progressLevel)
    }

    @Test
    fun `EC-090 - changing goal from higher to lower switches from NORMAL to APPROACHING`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        val entry = createEntry(id = 1, calories = 1400, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        // 1400/2000 = 70% -> NORMAL
        assertEquals(ProgressLevel.NORMAL, viewModel.uiState.value.calorieProgress.progressLevel)

        // Change goal to 1500 -> 1400/1500 = 93.3% -> APPROACHING
        val smallerGoal = CalorieGoalEntity(
            id = 4, name = "Light", calorieTarget = 1500,
            proteinTarget = null, fatTarget = null, carbsTarget = null,
            isActive = true, isPredefined = false
        )
        goalFlow.value = smallerGoal
        advanceUntilIdle()

        assertEquals(ProgressLevel.APPROACHING, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    @Test
    fun `EC-090 - removing goal mid-day sets progress to NO_GOAL`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 1000, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        assertEquals(ProgressLevel.NORMAL, viewModel.uiState.value.calorieProgress.progressLevel)

        // Remove goal
        goalFlow.value = null
        advanceUntilIdle()

        assertEquals(ProgressLevel.NO_GOAL, viewModel.uiState.value.calorieProgress.progressLevel)
        assertNull(viewModel.uiState.value.calorieProgress.progress)
    }

    // ============================================================
    // EC-091: Long decimals rounded to 1 decimal place
    // ============================================================

    @Test
    fun `EC-091 - totals remain rounded to 1 decimal with progress computation`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = FoodIntakeWithFood(
            entry = FoodIntakeEntryEntity(
                id = 1, foodId = 1, servings = 1.0,
                totalCalories = 100,
                totalProtein = 14.6666666,
                totalFat = 3.33333,
                totalCarbs = 42.555555,
                date = "2026-02-05", timestamp = System.currentTimeMillis()
            ),
            food = testFood
        )
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(14.7, state.totalProtein, 0.01)
        assertEquals(3.3, state.totalFat, 0.01)
        assertEquals(42.6, state.totalCarbs, 0.01)

        // Progress should be computed with rounded values
        assertNotNull(state.proteinProgress.progress)
        // 14.7/150 = 0.098
        assertEquals(0.098f, state.proteinProgress.progress!!, 0.001f)
    }

    // ============================================================
    // EC-093: Macro goals with some foods missing macro data
    // ============================================================

    @Test
    fun `EC-093 - foods with zero macros still compute progress correctly`() = runTest {
        goalFlow.value = goalWithMacros
        // Entry with 0 carbs (like chicken breast)
        val entry = createEntry(id = 1, calories = 165, protein = 31.0, fat = 3.6, carbs = 0.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0.0f, state.carbsProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.carbsProgress.progressLevel)
        assertEquals(200.0, state.carbsProgress.remaining!!, 0.01)
    }

    // ============================================================
    // Progress threshold boundary tests
    // ============================================================

    @Test
    fun `progress at 89_9 percent is NORMAL`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        // 1798/2000 = 89.9%
        val entry = createEntry(id = 1, calories = 1798, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        assertEquals(ProgressLevel.NORMAL, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    @Test
    fun `progress at 90_0 percent is APPROACHING`() = runTest {
        goalFlow.value = goalWithMacros // 2000 cal goal
        // 1800/2000 = 90.0%
        val entry = createEntry(id = 1, calories = 1800, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        assertEquals(ProgressLevel.APPROACHING, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    @Test
    fun `progress at 100_0 percent is APPROACHING (not EXCEEDED)`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 2000, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        assertEquals(ProgressLevel.APPROACHING, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    @Test
    fun `progress at 100_1 percent is EXCEEDED`() = runTest {
        goalFlow.value = goalWithMacros
        val entry = createEntry(id = 1, calories = 2001, protein = 50.0, fat = 30.0, carbs = 100.0)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        assertEquals(ProgressLevel.EXCEEDED, viewModel.uiState.value.calorieProgress.progressLevel)
    }

    // ============================================================
    // Multiple entries progress computation
    // ============================================================

    @Test
    fun `progress computed correctly with multiple entries`() = runTest {
        goalFlow.value = goalWithMacros
        val entry1 = createEntry(id = 1, calories = 500, protein = 30.0, fat = 15.0, carbs = 60.0)
        val entry2 = createEntry(id = 2, calories = 800, protein = 40.0, fat = 25.0, carbs = 90.0)
        val entry3 = createEntry(id = 3, calories = 600, protein = 35.0, fat = 20.0, carbs = 70.0)
        entriesFlow.value = listOf(entry1, entry2, entry3)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Total: 1900/2000 = 95% -> APPROACHING
        assertEquals(1900, state.totalCalories)
        assertEquals(0.95f, state.calorieProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.APPROACHING, state.calorieProgress.progressLevel)

        // Protein: 105/150 = 70% -> NORMAL
        assertEquals(105.0, state.totalProtein, 0.01)
        assertEquals(0.7f, state.proteinProgress.progress!!, 0.001f)
        assertEquals(ProgressLevel.NORMAL, state.proteinProgress.progressLevel)

        // Fat: 60/67 = ~89.6% -> NORMAL (just under 90%)
        assertEquals(60.0, state.totalFat, 0.01)
        assertEquals(ProgressLevel.NORMAL, state.fatProgress.progressLevel)

        // Carbs: 220/200 = 110% -> EXCEEDED
        assertEquals(220.0, state.totalCarbs, 0.01)
        assertEquals(ProgressLevel.EXCEEDED, state.carbsProgress.progressLevel)
    }

    // ============================================================
    // NutrientProgress data class tests
    // ============================================================

    @Test
    fun `NutrientProgress default values are correct`() {
        val progress = NutrientProgress()

        assertEquals(0.0, progress.consumed, 0.01)
        assertNull(progress.goal)
        assertNull(progress.progress)
        assertNull(progress.remaining)
        assertEquals(ProgressLevel.NORMAL, progress.progressLevel)
    }

    @Test
    fun `ProgressLevel enum has all expected values`() {
        val levels = ProgressLevel.values()
        assertEquals(4, levels.size)
        assertEquals(ProgressLevel.NORMAL, levels[0])
        assertEquals(ProgressLevel.APPROACHING, levels[1])
        assertEquals(ProgressLevel.EXCEEDED, levels[2])
        assertEquals(ProgressLevel.NO_GOAL, levels[3])
    }

    // ============================================================
    // Helper methods
    // ============================================================

    private fun createEntry(
        id: Long,
        calories: Int,
        protein: Double,
        fat: Double,
        carbs: Double
    ): FoodIntakeWithFood {
        return FoodIntakeWithFood(
            entry = FoodIntakeEntryEntity(
                id = id, foodId = 1, servings = 1.0,
                totalCalories = calories,
                totalProtein = protein,
                totalFat = fat,
                totalCarbs = carbs,
                date = "2026-02-05",
                timestamp = System.currentTimeMillis()
            ),
            food = testFood
        )
    }
}
