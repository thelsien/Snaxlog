package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.entity.FoodEntity
import io.mockk.coEvery
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

class FoodRepositoryImplTest {

    private lateinit var dao: FoodDao
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
        dao = mockk(relaxed = true)
        repository = FoodRepositoryImpl(dao)
    }

    @Test
    fun `getAllFoods returns all foods from dao`() = runTest {
        every { dao.getAllFoods() } returns flowOf(testFoods)

        val result = repository.getAllFoods().first()

        assertEquals(3, result.size)
        assertEquals("Apple", result[0].name)
        assertEquals("Grilled Chicken Breast", result[1].name)
        assertEquals("White Rice", result[2].name)
    }

    @Test
    fun `searchFoods returns matching foods`() = runTest {
        val chickenOnly = testFoods.filter { it.name.contains("Chicken", ignoreCase = true) }
        every { dao.searchFoods("chick") } returns flowOf(chickenOnly)

        val result = repository.searchFoods("chick").first()

        assertEquals(1, result.size)
        assertEquals("Grilled Chicken Breast", result[0].name)
    }

    @Test
    fun `searchFoods returns empty list for no matches`() = runTest {
        every { dao.searchFoods("pizza") } returns flowOf(emptyList())

        val result = repository.searchFoods("pizza").first()

        assertEquals(0, result.size)
    }

    @Test
    fun `getFoodById returns food when exists`() = runTest {
        coEvery { dao.getFoodById(1L) } returns testFoods[0]

        val result = repository.getFoodById(1L)

        assertNotNull(result)
        assertEquals("Apple", result!!.name)
        assertEquals(95, result.caloriesPerServing)
    }

    @Test
    fun `getFoodById returns null when not found`() = runTest {
        coEvery { dao.getFoodById(999L) } returns null

        val result = repository.getFoodById(999L)

        assertNull(result)
    }
}
