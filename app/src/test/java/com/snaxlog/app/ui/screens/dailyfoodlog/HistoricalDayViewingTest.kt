package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * FIP-EPIC-005: Historical Day Viewing Tests
 *
 * Tests for US-013 through US-017:
 * - US-013: Navigate to previous days
 * - US-014: View historical daily summary
 * - US-015: Edit entries on previous days
 * - US-016: Delete entries from previous days
 * - US-017: Add entries to previous days
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalDayViewingTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var foodIntakeRepository: FoodIntakeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var calorieGoalRepository: CalorieGoalRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: DailyFoodLogViewModel

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val testFood = FoodEntity(
        id = 1, name = "Apple", category = "Fruits",
        servingSize = "1 medium (182g)", servingWeightGrams = 182.0,
        caloriesPerServing = 95, proteinPerServing = 0.5,
        fatPerServing = 0.3, carbsPerServing = 25.1
    )

    private val testGoal = CalorieGoalEntity(
        id = 1, name = "Maintenance", calorieTarget = 2000,
        proteinTarget = 150.0, fatTarget = 67.0, carbsTarget = 200.0,
        isActive = true, isPredefined = true
    )

    private val entriesFlow = MutableStateFlow<List<FoodIntakeWithFood>>(emptyList())
    private val goalFlow = MutableStateFlow<CalorieGoalEntity?>(null)
    private val allFoodsFlow = MutableStateFlow(listOf(testFood))

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
        every { foodRepository.searchFoods(any()) } returns flowOf(listOf(testFood))

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
    // US-013: Navigate to previous days
    // ============================================================

    @Test
    fun `AC-058 - initial state is viewing today`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isViewingToday)
        assertEquals(LocalDate.now(), state.selectedDate)
        assertFalse(state.canNavigateForward)
    }

    @Test
    fun `AC-059 - navigate to previous day updates selected date`() = runTest {
        advanceUntilIdle()

        viewModel.navigateToPreviousDay()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(LocalDate.now().minusDays(1), state.selectedDate)
        assertFalse(state.isViewingToday)
        assertTrue(state.canNavigateForward)
    }

    @Test
    fun `AC-060 - navigate to next day from historical date moves forward`() = runTest {
        advanceUntilIdle()

        // Go back 2 days
        viewModel.navigateToPreviousDay()
        viewModel.navigateToPreviousDay()
        advanceUntilIdle()
        assertEquals(LocalDate.now().minusDays(2), viewModel.uiState.value.selectedDate)

        // Go forward 1 day
        viewModel.navigateToNextDay()
        advanceUntilIdle()

        assertEquals(LocalDate.now().minusDays(1), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `EC-095 - cannot navigate forward when viewing today`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isViewingToday)

        viewModel.navigateToNextDay()
        advanceUntilIdle()

        // Should still be on today
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isViewingToday)
    }

    @Test
    fun `AC-061 - return to today navigates directly to current day`() = runTest {
        advanceUntilIdle()

        // Navigate to historical date
        viewModel.navigateToPreviousDay()
        viewModel.navigateToPreviousDay()
        viewModel.navigateToPreviousDay()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isViewingToday)

        // Return to today
        viewModel.returnToToday()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isViewingToday)
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
    }

    @Test
    fun `EC-094 - can navigate to any past date`() = runTest {
        advanceUntilIdle()

        // Navigate far back (100 days)
        repeat(100) {
            viewModel.navigateToPreviousDay()
        }
        advanceUntilIdle()

        assertEquals(LocalDate.now().minusDays(100), viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.isViewingToday)
    }

    @Test
    fun `setSelectedDate sets specific date`() = runTest {
        advanceUntilIdle()
        val targetDate = LocalDate.now().minusDays(5)

        viewModel.setSelectedDate(targetDate)
        advanceUntilIdle()

        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.isViewingToday)
    }

    @Test
    fun `EC-123 - setSelectedDate prevents future dates`() = runTest {
        advanceUntilIdle()
        val futureDate = LocalDate.now().plusDays(5)

        viewModel.setSelectedDate(futureDate)
        advanceUntilIdle()

        // Should be clamped to today
        assertEquals(LocalDate.now(), viewModel.uiState.value.selectedDate)
        assertTrue(viewModel.uiState.value.isViewingToday)
    }

    @Test
    fun `EC-098 - selected date persists in SavedStateHandle`() = runTest {
        advanceUntilIdle()
        val targetDate = LocalDate.now().minusDays(3)

        viewModel.setSelectedDate(targetDate)
        advanceUntilIdle()

        // Verify SavedStateHandle has the date
        val savedDate = savedStateHandle.get<String>("selected_date")
        assertNotNull(savedDate)
        assertEquals(targetDate.format(dateFormatter), savedDate)
    }

    @Test
    fun `restores selected date from SavedStateHandle on creation`() = runTest {
        val previousDate = LocalDate.now().minusDays(7)
        val savedStateHandleWithDate = SavedStateHandle(
            mapOf("selected_date" to previousDate.format(dateFormatter))
        )

        val restoredViewModel = DailyFoodLogViewModel(
            foodIntakeRepository = foodIntakeRepository,
            foodRepository = foodRepository,
            calorieGoalRepository = calorieGoalRepository,
            savedStateHandle = savedStateHandleWithDate
        )

        advanceUntilIdle()

        assertEquals(previousDate, restoredViewModel.uiState.value.selectedDate)
        assertFalse(restoredViewModel.uiState.value.isViewingToday)
    }

    // ============================================================
    // US-014: View historical daily summary
    // ============================================================

    @Test
    fun `AC-062 - changing date loads entries for that date`() = runTest {
        val dateSlot = slot<String>()
        every { foodIntakeRepository.getEntriesForDate(capture(dateSlot)) } returns entriesFlow

        advanceUntilIdle()

        viewModel.navigateToPreviousDay()
        advanceUntilIdle()

        val expectedDate = LocalDate.now().minusDays(1).format(dateFormatter)
        assertEquals(expectedDate, dateSlot.captured)
    }

    @Test
    fun `isViewingToday is false when viewing historical date`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isViewingToday)

        viewModel.navigateToPreviousDay()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isViewingToday)
    }

    @Test
    fun `canNavigateForward is true when viewing historical date`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.canNavigateForward)

        viewModel.navigateToPreviousDay()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canNavigateForward)
    }

    // ============================================================
    // US-015: Edit entries on previous days
    // ============================================================

    @Test
    fun `AC-064 - loading historical entry sets isEditingHistorical flag`() = runTest {
        val historicalDate = LocalDate.now().minusDays(3)
        val historicalEntry = FoodIntakeEntryEntity(
            id = 1, foodId = 1, servings = 1.0,
            totalCalories = 95, totalProtein = 0.5, totalFat = 0.3, totalCarbs = 25.1,
            date = historicalDate.format(dateFormatter),
            timestamp = historicalDate.atTime(12, 0).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        )
        val entryWithFood = FoodIntakeWithFood(entry = historicalEntry, food = testFood)
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns entryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        val state = viewModel.editFoodState.value
        assertTrue(state.isEditingHistorical)
        assertEquals(historicalDate, state.entryDate)
    }

    @Test
    fun `loading today's entry does not set isEditingHistorical flag`() = runTest {
        val todayDate = LocalDate.now()
        val todayEntry = FoodIntakeEntryEntity(
            id = 1, foodId = 1, servings = 1.0,
            totalCalories = 95, totalProtein = 0.5, totalFat = 0.3, totalCarbs = 25.1,
            date = todayDate.format(dateFormatter),
            timestamp = System.currentTimeMillis()
        )
        val entryWithFood = FoodIntakeWithFood(entry = todayEntry, food = testFood)
        coEvery { foodIntakeRepository.getEntryWithFoodById(1L) } returns entryWithFood

        viewModel.loadEntryForEdit(1L)
        advanceUntilIdle()

        val state = viewModel.editFoodState.value
        assertFalse(state.isEditingHistorical)
        assertEquals(todayDate, state.entryDate)
    }

    // ============================================================
    // US-016: Delete entries from previous days (same as regular delete)
    // ============================================================

    @Test
    fun `deleting entry on historical date works same as today`() = runTest {
        val historicalDate = LocalDate.now().minusDays(5)
        val historicalEntry = FoodIntakeEntryEntity(
            id = 1, foodId = 1, servings = 1.0,
            totalCalories = 95, totalProtein = 0.5, totalFat = 0.3, totalCarbs = 25.1,
            date = historicalDate.format(dateFormatter),
            timestamp = historicalDate.atTime(12, 0).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        )
        val entryWithFood = FoodIntakeWithFood(entry = historicalEntry, food = testFood)

        // Navigate to historical date
        viewModel.setSelectedDate(historicalDate)
        advanceUntilIdle()

        // Delete entry
        viewModel.showDeleteDialog(entryWithFood)
        viewModel.confirmDeleteEntry()
        advanceUntilIdle()

        coVerify { foodIntakeRepository.deleteEntry(1L) }
        assertEquals("Entry deleted", viewModel.uiState.value.snackbarMessage)
    }

    // ============================================================
    // US-017: Add entries to previous days
    // ============================================================

    @Test
    fun `AC-066 - openAddFood on historical date sets isAddingToHistorical`() = runTest {
        val historicalDate = LocalDate.now().minusDays(3)
        viewModel.setSelectedDate(historicalDate)
        advanceUntilIdle()

        viewModel.openAddFood()

        val state = viewModel.addFoodState.value
        assertTrue(state.isAddingToHistorical)
        assertEquals(historicalDate, state.targetDate)
    }

    @Test
    fun `openAddFood on today does not set isAddingToHistorical`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isViewingToday)

        viewModel.openAddFood()

        val state = viewModel.addFoodState.value
        assertFalse(state.isAddingToHistorical)
        assertEquals(LocalDate.now(), state.targetDate)
    }

    @Test
    fun `AC-067 - no auto-category selection for historical dates`() = runTest {
        val historicalDate = LocalDate.now().minusDays(3)
        viewModel.setSelectedDate(historicalDate)
        advanceUntilIdle()

        viewModel.openAddFood()

        val state = viewModel.addFoodState.value
        assertNull(state.autoSelectedCategory)
        assertNull(state.selectedCategory)
    }

    @Test
    fun `auto-category selection works for today`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isViewingToday)

        viewModel.openAddFood()

        val state = viewModel.addFoodState.value
        assertNotNull(state.autoSelectedCategory)
        assertNotNull(state.selectedCategory)
    }

    @Test
    fun `EC-122 - saving entry on historical date uses target date`() = runTest {
        val historicalDate = LocalDate.now().minusDays(5)
        coEvery { foodIntakeRepository.addEntry(any()) } returns 1L

        viewModel.setSelectedDate(historicalDate)
        advanceUntilIdle()

        viewModel.openAddFood()
        viewModel.selectFood(testFood)
        viewModel.saveAddFood()
        advanceUntilIdle()

        val expectedDateString = historicalDate.format(dateFormatter)
        coVerify {
            foodIntakeRepository.addEntry(match { entry ->
                entry.date == expectedDateString
            })
        }
    }

    // ============================================================
    // Date Picker Dialog
    // ============================================================

    @Test
    fun `openDatePicker sets isDatePickerOpen to true`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDatePickerOpen)

        viewModel.openDatePicker()

        assertTrue(viewModel.uiState.value.isDatePickerOpen)
    }

    @Test
    fun `closeDatePicker sets isDatePickerOpen to false`() = runTest {
        advanceUntilIdle()
        viewModel.openDatePicker()
        assertTrue(viewModel.uiState.value.isDatePickerOpen)

        viewModel.closeDatePicker()

        assertFalse(viewModel.uiState.value.isDatePickerOpen)
    }

    @Test
    fun `onDatePickerDateSelected updates date and closes picker`() = runTest {
        advanceUntilIdle()
        viewModel.openDatePicker()
        assertTrue(viewModel.uiState.value.isDatePickerOpen)

        val targetDate = LocalDate.now().minusDays(10)
        viewModel.onDatePickerDateSelected(targetDate)
        advanceUntilIdle()

        assertEquals(targetDate, viewModel.uiState.value.selectedDate)
        assertFalse(viewModel.uiState.value.isDatePickerOpen)
    }
}
