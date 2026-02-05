package com.snaxlog.app.ui.screens.dailyfoodlog

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Focused tests for US-004: Delete food intake entry.
 * Covers AC-016 through AC-019 and related edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteEntryTest {

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

    private fun makeEntry(id: Long, food: FoodEntity, servings: Double = 1.0): FoodIntakeWithFood {
        return FoodIntakeWithFood(
            entry = FoodIntakeEntryEntity(
                id = id, foodId = food.id, servings = servings,
                totalCalories = (food.caloriesPerServing * servings).toInt(),
                totalProtein = food.proteinPerServing * servings,
                totalFat = food.fatPerServing * servings,
                totalCarbs = food.carbsPerServing * servings,
                date = "2026-02-04", timestamp = System.currentTimeMillis() + id
            ),
            food = food
        )
    }

    private val entriesFlow = MutableStateFlow<List<FoodIntakeWithFood>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodIntakeRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)
        calorieGoalRepository = mockk(relaxed = true)

        every { foodIntakeRepository.getEntriesForDate(any()) } returns entriesFlow
        every { calorieGoalRepository.getActiveGoal() } returns flowOf(null)
        every { foodRepository.getAllFoods() } returns flowOf(listOf(testFood, testFood2))

        viewModel = DailyFoodLogViewModel(foodIntakeRepository, foodRepository, calorieGoalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `AC-016 - tapping delete shows confirmation dialog with entry info`() = runTest {
        val entry = makeEntry(1, testFood)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        viewModel.showDeleteDialog(entry)

        val state = viewModel.uiState.value
        assertNotNull(state.deleteDialogEntry)
        assertEquals(1L, state.deleteDialogEntry!!.entry.id)
        assertEquals("Apple", state.deleteDialogEntry!!.food.name)
    }

    @Test
    fun `AC-017 - confirming deletion calls repository delete`() = runTest {
        val entry = makeEntry(1, testFood)
        viewModel.showDeleteDialog(entry)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        coVerify { foodIntakeRepository.deleteEntry(1L) }
    }

    @Test
    fun `AC-018 - after deletion entry is removed and totals recalculated`() = runTest {
        val entry1 = makeEntry(1, testFood) // 95 cal
        val entry2 = makeEntry(2, testFood2) // 165 cal
        entriesFlow.value = listOf(entry1, entry2)
        advanceUntilIdle()
        assertEquals(260, viewModel.uiState.value.totalCalories)

        // Simulate deletion: remove entry1 from flow
        viewModel.showDeleteDialog(entry1)
        viewModel.confirmDeleteEntry()
        entriesFlow.value = listOf(entry2)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.entries.size)
        assertEquals(165, viewModel.uiState.value.totalCalories)
    }

    @Test
    fun `AC-019 - cancelling deletion keeps entry unchanged`() = runTest {
        val entry = makeEntry(1, testFood)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        viewModel.showDeleteDialog(entry)
        assertNotNull(viewModel.uiState.value.deleteDialogEntry)

        viewModel.dismissDeleteDialog()
        assertNull(viewModel.uiState.value.deleteDialogEntry)

        // Entry should still be present
        assertEquals(1, viewModel.uiState.value.entries.size)
        coVerify(exactly = 0) { foodIntakeRepository.deleteEntry(any()) }
    }

    @Test
    fun `EC-031 - deleting only entry shows empty state and resets totals`() = runTest {
        val entry = makeEntry(1, testFood)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.entries.size)
        assertEquals(95, viewModel.uiState.value.totalCalories)

        viewModel.showDeleteDialog(entry)
        viewModel.confirmDeleteEntry()
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.entries.isEmpty())
        assertEquals(0, viewModel.uiState.value.totalCalories)
        assertEquals(0.0, viewModel.uiState.value.totalProtein, 0.01)
        assertEquals(0.0, viewModel.uiState.value.totalFat, 0.01)
        assertEquals(0.0, viewModel.uiState.value.totalCarbs, 0.01)
    }

    @Test
    fun `EC-033 - database deletion failure shows error and keeps entry`() = runTest {
        val entry = makeEntry(1, testFood)
        entriesFlow.value = listOf(entry)
        advanceUntilIdle()

        coEvery { foodIntakeRepository.deleteEntry(any()) } throws RuntimeException("DB error")

        viewModel.showDeleteDialog(entry)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        // Error should be reported, dialog dismissed
        assertNull(viewModel.uiState.value.deleteDialogEntry)
        assertNotNull(viewModel.uiState.value.error)

        // Entry should still be in list (flow unchanged)
        assertEquals(1, viewModel.uiState.value.entries.size)
    }

    @Test
    fun `EC-034 - rapid deletions of multiple entries process correctly`() = runTest {
        val entry1 = makeEntry(1, testFood)
        val entry2 = makeEntry(2, testFood2)
        entriesFlow.value = listOf(entry1, entry2)
        advanceUntilIdle()

        // Delete first entry
        viewModel.showDeleteDialog(entry1)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()
        entriesFlow.value = listOf(entry2)
        advanceUntilIdle()

        coVerify { foodIntakeRepository.deleteEntry(1L) }
        assertEquals(1, viewModel.uiState.value.entries.size)

        // Delete second entry
        viewModel.showDeleteDialog(entry2)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()
        entriesFlow.value = emptyList()
        advanceUntilIdle()

        coVerify { foodIntakeRepository.deleteEntry(2L) }
        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }

    @Test
    fun `EC-035 - dismissing dialog by tapping outside does not delete`() = runTest {
        val entry = makeEntry(1, testFood)
        viewModel.showDeleteDialog(entry)
        viewModel.dismissDeleteDialog() // simulates tapping outside

        assertNull(viewModel.uiState.value.deleteDialogEntry)
        coVerify(exactly = 0) { foodIntakeRepository.deleteEntry(any()) }
    }

    @Test
    fun `snackbar shows Entry deleted message after successful deletion`() = runTest {
        val entry = makeEntry(1, testFood)
        viewModel.showDeleteDialog(entry)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        assertEquals("Entry deleted", viewModel.uiState.value.snackbarMessage)
    }
}
