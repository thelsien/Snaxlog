package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalorieGoalRepositoryImplTest {

    private lateinit var dao: CalorieGoalDao
    private lateinit var repository: CalorieGoalRepositoryImpl

    private val predefinedGoal = CalorieGoalEntity(
        id = 1, name = "Weight Loss", calorieTarget = 1500,
        proteinTarget = 120.0, fatTarget = 50.0, carbsTarget = 150.0,
        isActive = false, isPredefined = true
    )

    private val maintenanceGoal = CalorieGoalEntity(
        id = 2, name = "Maintenance", calorieTarget = 2000,
        proteinTarget = 150.0, fatTarget = 67.0, carbsTarget = 200.0,
        isActive = true, isPredefined = true
    )

    private val customGoal = CalorieGoalEntity(
        id = 10, name = "My Custom Goal", calorieTarget = 1800,
        proteinTarget = 130.0, fatTarget = 60.0, carbsTarget = 180.0,
        isActive = false, isPredefined = false
    )

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repository = CalorieGoalRepositoryImpl(dao)
    }

    @Test
    fun `getAllGoals returns all goals from dao`() = runTest {
        every { dao.getAllGoals() } returns flowOf(listOf(predefinedGoal, maintenanceGoal, customGoal))

        val result = repository.getAllGoals().first()

        assertEquals(3, result.size)
        assertEquals("Weight Loss", result[0].name)
        assertEquals("Maintenance", result[1].name)
        assertEquals("My Custom Goal", result[2].name)
    }

    @Test
    fun `getActiveGoal returns active goal from dao`() = runTest {
        every { dao.getActiveGoal() } returns flowOf(maintenanceGoal)

        val result = repository.getActiveGoal().first()

        assertNotNull(result)
        assertEquals(2L, result!!.id)
        assertTrue(result.isActive)
    }

    @Test
    fun `getActiveGoal returns null when no goal is active`() = runTest {
        every { dao.getActiveGoal() } returns flowOf(null)

        val result = repository.getActiveGoal().first()

        assertNull(result)
    }

    @Test
    fun `getGoalById returns goal when exists`() = runTest {
        coEvery { dao.getGoalById(10L) } returns customGoal

        val result = repository.getGoalById(10L)

        assertNotNull(result)
        assertEquals("My Custom Goal", result!!.name)
        assertEquals(1800, result.calorieTarget)
    }

    @Test
    fun `getGoalById returns null when not found`() = runTest {
        coEvery { dao.getGoalById(999L) } returns null

        val result = repository.getGoalById(999L)

        assertNull(result)
    }

    @Test
    fun `setActiveGoal deactivates all then activates specified goal`() = runTest {
        repository.setActiveGoal(10L)

        coVerify(ordering = io.mockk.Ordering.ORDERED) {
            dao.deactivateAllGoals()
            dao.activateGoal(10L)
        }
    }

    @Test
    fun `addGoal calls dao insertGoal and returns id`() = runTest {
        coEvery { dao.insertGoal(any()) } returns 10L

        val result = repository.addGoal(customGoal)

        assertEquals(10L, result)
        coVerify { dao.insertGoal(customGoal) }
    }

    @Test
    fun `updateGoal calls dao updateGoal`() = runTest {
        val updated = customGoal.copy(name = "Updated Goal", calorieTarget = 1900)

        repository.updateGoal(updated)

        coVerify { dao.updateGoal(updated) }
    }

    @Test
    fun `deleteGoal calls dao deleteGoal`() = runTest {
        repository.deleteGoal(10L)

        coVerify { dao.deleteGoal(10L) }
    }
}
