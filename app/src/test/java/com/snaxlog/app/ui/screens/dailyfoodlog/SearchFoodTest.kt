package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Focused tests for US-005: Search food database.
 * Covers AC-020 through AC-024 and related edge cases.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchFoodTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var foodIntakeRepository: FoodIntakeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var calorieGoalRepository: CalorieGoalRepository
    private lateinit var viewModel: DailyFoodLogViewModel

    private val apple = FoodEntity(
        id = 1, name = "Apple", category = "Fruits",
        servingSize = "1 medium (182g)", servingWeightGrams = 182.0,
        caloriesPerServing = 95, proteinPerServing = 0.5,
        fatPerServing = 0.3, carbsPerServing = 25.1
    )

    private val chicken = FoodEntity(
        id = 2, name = "Grilled Chicken Breast", category = "Protein",
        servingSize = "1 breast (100g)", servingWeightGrams = 100.0,
        caloriesPerServing = 165, proteinPerServing = 31.0,
        fatPerServing = 3.6, carbsPerServing = 0.0
    )

    private val rice = FoodEntity(
        id = 3, name = "White Rice", category = "Grains",
        servingSize = "1 cup cooked (158g)", servingWeightGrams = 158.0,
        caloriesPerServing = 206, proteinPerServing = 4.3,
        fatPerServing = 0.4, carbsPerServing = 44.5
    )

    private val allFoods = listOf(apple, chicken, rice)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodIntakeRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)
        calorieGoalRepository = mockk(relaxed = true)

        every { foodIntakeRepository.getEntriesForDate(any()) } returns flowOf(emptyList())
        every { calorieGoalRepository.getActiveGoal() } returns flowOf(null)
        every { foodRepository.getAllFoods() } returns flowOf(allFoods)
        every { foodRepository.searchFoods("apple") } returns flowOf(listOf(apple))
        every { foodRepository.searchFoods("chick") } returns flowOf(listOf(chicken))
        every { foodRepository.searchFoods("rice") } returns flowOf(listOf(rice))
        every { foodRepository.searchFoods("pizza") } returns flowOf(emptyList())
        every { foodRepository.searchFoods("!@#\$%") } returns flowOf(emptyList())

        viewModel = DailyFoodLogViewModel(foodIntakeRepository, foodRepository, calorieGoalRepository, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `AC-020 - typing in search filters food list in real-time`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        // Initially shows all foods
        assertEquals(3, viewModel.addFoodState.value.foods.size)

        // Type search query
        viewModel.updateSearchQuery("apple")
        advanceUntilIdle()

        // Should filter to just Apple
        assertEquals(1, viewModel.addFoodState.value.foods.size)
        assertEquals("Apple", viewModel.addFoodState.value.foods[0].name)
    }

    @Test
    fun `AC-021 - partial word matches foods where any word starts with search term`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("chick")
        advanceUntilIdle()

        assertEquals(1, viewModel.addFoodState.value.foods.size)
        assertEquals("Grilled Chicken Breast", viewModel.addFoodState.value.foods[0].name)
    }

    @Test
    fun `AC-022 - search results show food name, serving size, and calories`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("apple")
        advanceUntilIdle()

        val food = viewModel.addFoodState.value.foods[0]
        assertEquals("Apple", food.name)
        assertEquals("1 medium (182g)", food.servingSize)
        assertEquals(95, food.caloriesPerServing)
    }

    @Test
    fun `AC-023 - no matches shows empty food list`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("pizza")
        advanceUntilIdle()

        assertTrue(viewModel.addFoodState.value.foods.isEmpty())
    }

    @Test
    fun `AC-024 - clearing search shows all foods again`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()
        assertEquals(3, viewModel.addFoodState.value.foods.size)

        viewModel.updateSearchQuery("apple")
        advanceUntilIdle()
        assertEquals(1, viewModel.addFoodState.value.foods.size)

        viewModel.clearSearch()
        advanceUntilIdle()

        assertEquals("", viewModel.addFoodState.value.searchQuery)
        assertEquals(3, viewModel.addFoodState.value.foods.size)
    }

    @Test
    fun `EC-038 - special characters in search are handled gracefully`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("!@#\$%")
        advanceUntilIdle()

        // Should return empty list without crashing
        assertTrue(viewModel.addFoodState.value.foods.isEmpty())
    }

    @Test
    fun `EC-039 - whitespace-only search treated as empty and shows all foods`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("   ")
        advanceUntilIdle()

        // Whitespace is blank, so getAllFoods should be called
        assertEquals(3, viewModel.addFoodState.value.foods.size)
    }

    @Test
    fun `EC-040 - search input capped at 100 characters`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        val longQuery = "a".repeat(200)
        viewModel.updateSearchQuery(longQuery)

        assertEquals(100, viewModel.addFoodState.value.searchQuery.length)
    }

    @Test
    fun `EC-041 - search is case-insensitive (handled by DAO)`() = runTest {
        // This is handled at the DAO/SQL level with LIKE operator.
        // Here we verify the query is passed through correctly.
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.updateSearchQuery("apple")
        advanceUntilIdle()

        // Verify searchFoods was called with the trimmed query
        verify { foodRepository.searchFoods("apple") }
    }

    @Test
    fun `EC-045 - unicode characters in search are handled gracefully`() = runTest {
        every { foodRepository.searchFoods(any()) } returns flowOf(emptyList())

        viewModel.openAddFood()
        advanceUntilIdle()

        // Should not crash
        viewModel.updateSearchQuery("\uD83C\uDF4E") // apple emoji
        advanceUntilIdle()

        assertTrue(viewModel.addFoodState.value.foods.isEmpty())
    }

    @Test
    fun `selecting food from search results transitions to serving input`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.selectFood(apple)

        val state = viewModel.addFoodState.value
        assertEquals(apple, state.selectedFood)
        assertEquals("1.0", state.servingsInput)
        assertNull(state.servingsError)
    }

    @Test
    fun `clearing food selection returns to search view`() = runTest {
        viewModel.openAddFood()
        advanceUntilIdle()

        viewModel.selectFood(apple)
        assertEquals(apple, viewModel.addFoodState.value.selectedFood)

        viewModel.clearFoodSelection()
        assertNull(viewModel.addFoodState.value.selectedFood)
    }
}
