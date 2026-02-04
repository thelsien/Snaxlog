package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodIntakeEntryDao
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FoodIntakeRepositoryImplTest {

    private lateinit var dao: FoodIntakeEntryDao
    private lateinit var repository: FoodIntakeRepositoryImpl

    private val testFood = FoodEntity(
        id = 1,
        name = "Apple",
        category = "Fruits",
        servingSize = "1 medium (182g)",
        servingWeightGrams = 182.0,
        caloriesPerServing = 95,
        proteinPerServing = 0.5,
        fatPerServing = 0.3,
        carbsPerServing = 25.1
    )

    private val testEntry = FoodIntakeEntryEntity(
        id = 1,
        foodId = 1,
        servings = 2.0,
        totalCalories = 190,
        totalProtein = 1.0,
        totalFat = 0.6,
        totalCarbs = 50.2,
        date = "2026-02-04",
        timestamp = 1738684800000L
    )

    private val testEntryWithFood = FoodIntakeWithFood(
        entry = testEntry,
        food = testFood
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = FoodIntakeRepositoryImpl(dao)
    }

    @Test
    fun `getEntriesForDate returns entries from dao`() = runTest {
        every { dao.getEntriesForDate("2026-02-04") } returns flowOf(listOf(testEntryWithFood))

        val result = repository.getEntriesForDate("2026-02-04").first()

        assertEquals(1, result.size)
        assertEquals("Apple", result[0].food.name)
        assertEquals(190, result[0].entry.totalCalories)
    }

    @Test
    fun `getEntriesForDate returns empty list when no entries`() = runTest {
        every { dao.getEntriesForDate("2026-02-04") } returns flowOf(emptyList())

        val result = repository.getEntriesForDate("2026-02-04").first()

        assertEquals(0, result.size)
    }

    @Test
    fun `getEntryWithFoodById returns entry when exists`() = runTest {
        coEvery { dao.getEntryWithFoodById(1L) } returns testEntryWithFood

        val result = repository.getEntryWithFoodById(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.entry.id)
        assertEquals("Apple", result.food.name)
    }

    @Test
    fun `getEntryWithFoodById returns null when not found`() = runTest {
        coEvery { dao.getEntryWithFoodById(999L) } returns null

        val result = repository.getEntryWithFoodById(999L)

        assertNull(result)
    }

    @Test
    fun `addEntry calls dao insertEntry`() = runTest {
        coEvery { dao.insertEntry(testEntry) } returns 1L

        val result = repository.addEntry(testEntry)

        assertEquals(1L, result)
        coVerify { dao.insertEntry(testEntry) }
    }

    @Test
    fun `updateEntry calls dao updateEntry`() = runTest {
        val updated = testEntry.copy(servings = 3.0, totalCalories = 285)

        repository.updateEntry(updated)

        coVerify { dao.updateEntry(updated) }
    }

    @Test
    fun `deleteEntry calls dao deleteEntry`() = runTest {
        repository.deleteEntry(1L)

        coVerify { dao.deleteEntry(1L) }
    }

    @Test
    fun `getTotalCaloriesForDate returns sum from dao`() = runTest {
        every { dao.getTotalCaloriesForDate("2026-02-04") } returns flowOf(285)

        val result = repository.getTotalCaloriesForDate("2026-02-04").first()

        assertEquals(285, result)
    }

    @Test
    fun `getTotalProteinForDate returns sum from dao`() = runTest {
        every { dao.getTotalProteinForDate("2026-02-04") } returns flowOf(42.5)

        val result = repository.getTotalProteinForDate("2026-02-04").first()

        assertEquals(42.5, result, 0.01)
    }

    @Test
    fun `getTotalFatForDate returns sum from dao`() = runTest {
        every { dao.getTotalFatForDate("2026-02-04") } returns flowOf(12.3)

        val result = repository.getTotalFatForDate("2026-02-04").first()

        assertEquals(12.3, result, 0.01)
    }

    @Test
    fun `getTotalCarbsForDate returns sum from dao`() = runTest {
        every { dao.getTotalCarbsForDate("2026-02-04") } returns flowOf(100.0)

        val result = repository.getTotalCarbsForDate("2026-02-04").first()

        assertEquals(100.0, result, 0.01)
    }
}
