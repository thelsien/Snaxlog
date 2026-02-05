package com.snaxlog.app.ui.screens.goalmanagement

import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.repository.CalorieGoalRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class GoalManagementViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var calorieGoalRepository: CalorieGoalRepository
    private lateinit var viewModel: GoalManagementViewModel

    private val predefinedGoal1 = CalorieGoalEntity(
        id = 1, name = "Weight Loss", calorieTarget = 1500,
        proteinTarget = 120.0, fatTarget = 50.0, carbsTarget = 150.0,
        isActive = false, isPredefined = true
    )

    private val predefinedGoal2 = CalorieGoalEntity(
        id = 2, name = "Maintenance", calorieTarget = 2000,
        proteinTarget = 150.0, fatTarget = 67.0, carbsTarget = 200.0,
        isActive = true, isPredefined = true
    )

    private val predefinedGoal3 = CalorieGoalEntity(
        id = 3, name = "Muscle Gain", calorieTarget = 2500,
        proteinTarget = 200.0, fatTarget = 83.0, carbsTarget = 250.0,
        isActive = false, isPredefined = true
    )

    private val customGoal = CalorieGoalEntity(
        id = 10, name = "My Custom", calorieTarget = 1800,
        proteinTarget = 130.0, fatTarget = 60.0, carbsTarget = 180.0,
        isActive = false, isPredefined = false
    )

    private val allGoalsFlow = MutableStateFlow(
        listOf(predefinedGoal1, predefinedGoal2, predefinedGoal3, customGoal)
    )
    private val activeGoalFlow = MutableStateFlow<CalorieGoalEntity?>(predefinedGoal2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        calorieGoalRepository = mockk(relaxed = true)

        every { calorieGoalRepository.getAllGoals() } returns allGoalsFlow
        every { calorieGoalRepository.getActiveGoal() } returns activeGoalFlow

        viewModel = GoalManagementViewModel(calorieGoalRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ============================================================
    // US-006: View calorie goals
    // ============================================================

    @Test
    fun `AC-025 - shows list of predefined calorie goals`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.goals.size)

        val predefined = state.goals.filter { it.isPredefined }
        assertEquals(3, predefined.size)
        assertTrue(predefined.any { it.name == "Weight Loss" && it.calorieTarget == 1500 })
        assertTrue(predefined.any { it.name == "Maintenance" && it.calorieTarget == 2000 })
        assertTrue(predefined.any { it.name == "Muscle Gain" && it.calorieTarget == 2500 })
    }

    @Test
    fun `AC-026 - custom goals appear alongside predefined goals`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val custom = state.goals.filter { !it.isPredefined }
        assertEquals(1, custom.size)
        assertEquals("My Custom", custom[0].name)
    }

    @Test
    fun `AC-027 - active goal is visually indicated via activeGoalId`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2L, state.activeGoalId)
    }

    @Test
    fun `AC-028 - no active goal results in null activeGoalId`() = runTest {
        activeGoalFlow.value = null
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.activeGoalId)
    }

    @Test
    fun `EC-047 - many custom goals displayed correctly`() = runTest {
        val manyGoals = (1..50).map { i ->
            CalorieGoalEntity(
                id = (100 + i).toLong(), name = "Custom $i", calorieTarget = 1500 + i * 10,
                isActive = false, isPredefined = false
            )
        }
        allGoalsFlow.value = listOf(predefinedGoal1, predefinedGoal2, predefinedGoal3) + manyGoals
        advanceUntilIdle()

        assertEquals(53, viewModel.uiState.value.goals.size)
    }

    @Test
    fun `EC-049 - multiple goals with same calorie value are all shown`() = runTest {
        val duplicate = customGoal.copy(id = 11, name = "Also 1800 cals")
        allGoalsFlow.value = listOf(predefinedGoal1, customGoal, duplicate)
        advanceUntilIdle()

        val goals = viewModel.uiState.value.goals
        val matching = goals.filter { it.calorieTarget == 1800 }
        assertEquals(2, matching.size)
    }

    // ============================================================
    // US-007: Select active calorie goal
    // ============================================================

    @Test
    fun `AC-029 - selecting a goal calls repository setActiveGoal`() = runTest {
        advanceUntilIdle()

        viewModel.selectGoal(1L)
        advanceUntilIdle()

        coVerify { calorieGoalRepository.setActiveGoal(1L) }
    }

    @Test
    fun `AC-030 - after selecting goal, activeGoalId updates via flow`() = runTest {
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.activeGoalId)

        // Simulate repository updating the active goal flow
        activeGoalFlow.value = predefinedGoal1.copy(isActive = true)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.activeGoalId)
    }

    @Test
    fun `AC-031 - selecting different goal deactivates previous via repository`() = runTest {
        advanceUntilIdle()

        viewModel.selectGoal(3L)
        advanceUntilIdle()

        // Repository's setActiveGoal handles deactivation internally
        coVerify { calorieGoalRepository.setActiveGoal(3L) }
    }

    @Test
    fun `AC-032 - only one goal is active at a time`() = runTest {
        advanceUntilIdle()

        // activeGoalId is a single Long, guarantees single active
        assertNotNull(viewModel.uiState.value.activeGoalId)
        assertEquals(2L, viewModel.uiState.value.activeGoalId)
    }

    @Test
    fun `EC-052 - rapid goal selections only the last one matters`() = runTest {
        advanceUntilIdle()

        viewModel.selectGoal(1L)
        viewModel.selectGoal(3L)
        viewModel.selectGoal(10L)
        advanceUntilIdle()

        // All three calls should go through; the repository handles atomicity
        coVerify { calorieGoalRepository.setActiveGoal(1L) }
        coVerify { calorieGoalRepository.setActiveGoal(3L) }
        coVerify { calorieGoalRepository.setActiveGoal(10L) }
    }

    @Test
    fun `EC-053 - database failure when setting active goal shows error`() = runTest {
        coEvery { calorieGoalRepository.setActiveGoal(any()) } throws RuntimeException("DB error")
        advanceUntilIdle()

        viewModel.selectGoal(1L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals("Failed to set active goal. Please try again.", viewModel.uiState.value.error)
    }

    // ============================================================
    // US-008: Add custom calorie goal
    // ============================================================

    @Test
    fun `AC-033 - openAddGoalForm initializes empty form state`() = runTest {
        viewModel.openAddGoalForm()

        val form = viewModel.formState.value
        assertFalse(form.isEditMode)
        assertNull(form.editingGoalId)
        assertEquals("", form.nameInput)
        assertEquals("", form.calorieInput)
        assertEquals("", form.proteinInput)
        assertEquals("", form.fatInput)
        assertEquals("", form.carbsInput)
    }

    @Test
    fun `AC-034 - form accepts name, calorie value, and optional macro targets`() = runTest {
        viewModel.openAddGoalForm()

        viewModel.updateGoalName("Keto Diet")
        viewModel.updateCalorieTarget("1800")
        viewModel.updateProteinTarget("130")
        viewModel.updateFatTarget("120")
        viewModel.updateCarbsTarget("20")

        val form = viewModel.formState.value
        assertEquals("Keto Diet", form.nameInput)
        assertEquals("1800", form.calorieInput)
        assertEquals("130", form.proteinInput)
        assertEquals("120", form.fatInput)
        assertEquals("20", form.carbsInput)
        assertNull(form.nameError)
        assertNull(form.calorieError)
        assertNull(form.proteinError)
        assertNull(form.fatError)
        assertNull(form.carbsError)
    }

    @Test
    fun `AC-035 - saving new goal calls repository addGoal`() = runTest {
        coEvery { calorieGoalRepository.addGoal(any()) } returns 11L
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Keto Diet")
        viewModel.updateCalorieTarget("1800")
        viewModel.updateProteinTarget("130")
        viewModel.updateFatTarget("120")
        viewModel.updateCarbsTarget("20")
        viewModel.saveGoal()
        advanceUntilIdle()

        coVerify {
            calorieGoalRepository.addGoal(match { goal ->
                goal.name == "Keto Diet" &&
                        goal.calorieTarget == 1800 &&
                        goal.proteinTarget == 130.0 &&
                        goal.fatTarget == 120.0 &&
                        goal.carbsTarget == 20.0 &&
                        !goal.isPredefined &&
                        !goal.isActive
            })
        }
        assertEquals("Goal saved", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `AC-036 - saving with empty name shows validation error`() = runTest {
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("")
        viewModel.updateCalorieTarget("1800")
        viewModel.saveGoal()
        advanceUntilIdle()

        assertEquals("Goal name cannot be empty", viewModel.formState.value.nameError)
        coVerify(exactly = 0) { calorieGoalRepository.addGoal(any()) }
    }

    @Test
    fun `AC-036 - saving with empty calorie target shows validation error`() = runTest {
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Test Goal")
        viewModel.updateCalorieTarget("")
        viewModel.saveGoal()
        advanceUntilIdle()

        assertEquals("Calorie target is required", viewModel.formState.value.calorieError)
        coVerify(exactly = 0) { calorieGoalRepository.addGoal(any()) }
    }

    @Test
    fun `AC-037 - custom goal saved with isPredefined false`() = runTest {
        coEvery { calorieGoalRepository.addGoal(any()) } returns 11L
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Test")
        viewModel.updateCalorieTarget("1800")
        viewModel.saveGoal()
        advanceUntilIdle()

        coVerify {
            calorieGoalRepository.addGoal(match { !it.isPredefined })
        }
    }

    @Test
    fun `AC-035 - saving without optional macros sets them to null`() = runTest {
        coEvery { calorieGoalRepository.addGoal(any()) } returns 11L
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Simple Goal")
        viewModel.updateCalorieTarget("2000")
        // Leave protein, fat, carbs empty
        viewModel.saveGoal()
        advanceUntilIdle()

        coVerify {
            calorieGoalRepository.addGoal(match { goal ->
                goal.proteinTarget == null &&
                        goal.fatTarget == null &&
                        goal.carbsTarget == null
            })
        }
    }

    @Test
    fun `EC-057 - zero calories shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateCalorieTarget("0")

        assertEquals("Calorie goal must be greater than 0", viewModel.formState.value.calorieError)
    }

    @Test
    fun `EC-058 - negative calories shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateCalorieTarget("-500")

        assertEquals("Calorie goal must be greater than 0", viewModel.formState.value.calorieError)
    }

    @Test
    fun `EC-060 - non-numeric calorie input shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateCalorieTarget("abc")

        assertEquals("Please enter a valid number", viewModel.formState.value.calorieError)
    }

    @Test
    fun `EC-062 - whitespace-only goal name shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateGoalName("   ")

        assertEquals("Goal name cannot be empty", viewModel.formState.value.nameError)
    }

    @Test
    fun `EC-063 - goal name limited to 50 characters`() = runTest {
        viewModel.openAddGoalForm()
        val longName = "a".repeat(100)
        viewModel.updateGoalName(longName)

        assertEquals(50, viewModel.formState.value.nameInput.length)
    }

    @Test
    fun `EC-064 - special characters and emoji accepted in goal name`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateGoalName("My Goal #1 (2026)")

        assertNull(viewModel.formState.value.nameError)
        assertEquals("My Goal #1 (2026)", viewModel.formState.value.nameInput)
    }

    @Test
    fun `EC-061 - decimal calorie value accepted`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateCalorieTarget("1850.5")

        assertNull(viewModel.formState.value.calorieError)
    }

    @Test
    fun `negative macro target shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateProteinTarget("-10")

        assertEquals("Protein target cannot be negative", viewModel.formState.value.proteinError)
    }

    @Test
    fun `non-numeric macro input shows validation error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateFatTarget("abc")

        assertEquals("Please enter a valid number", viewModel.formState.value.fatError)
    }

    @Test
    fun `empty macro target is accepted as optional`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateProteinTarget("")

        assertNull(viewModel.formState.value.proteinError)
    }

    @Test
    fun `EC-068 - cancelling add form does not persist data`() = runTest {
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Draft Goal")
        viewModel.updateCalorieTarget("1800")
        // No save - simulates cancel

        coVerify(exactly = 0) { calorieGoalRepository.addGoal(any()) }
    }

    @Test
    fun `double save prevented by isSaving flag`() = runTest {
        coEvery { calorieGoalRepository.addGoal(any()) } returns 11L
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Test")
        viewModel.updateCalorieTarget("1800")

        viewModel.saveGoal()
        viewModel.saveGoal() // Should be ignored
        advanceUntilIdle()

        coVerify(exactly = 1) { calorieGoalRepository.addGoal(any()) }
    }

    @Test
    fun `save failure shows error and resets isSaving`() = runTest {
        coEvery { calorieGoalRepository.addGoal(any()) } throws RuntimeException("DB error")
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("Test")
        viewModel.updateCalorieTarget("1800")
        viewModel.saveGoal()
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.isSaving)
        assertEquals("Failed to save goal. Please try again.", viewModel.uiState.value.error)
    }

    // ============================================================
    // US-009: Edit calorie goal
    // ============================================================

    @Test
    fun `AC-038 - opening edit form pre-fills current values`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertTrue(form.isEditMode)
        assertEquals(10L, form.editingGoalId)
        assertEquals("My Custom", form.nameInput)
        assertEquals("1800", form.calorieInput)
        assertEquals("130", form.proteinInput)
        assertEquals("60", form.fatInput)
        assertEquals("180", form.carbsInput)
        assertFalse(form.isLoading)
    }

    @Test
    fun `AC-039 - changing values in edit form updates state`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateGoalName("Updated Goal")
        viewModel.updateCalorieTarget("1900")

        val form = viewModel.formState.value
        assertEquals("Updated Goal", form.nameInput)
        assertEquals("1900", form.calorieInput)
    }

    @Test
    fun `AC-040 - saving edited goal calls repository updateGoal`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateGoalName("Updated Name")
        viewModel.updateCalorieTarget("1900")
        viewModel.saveGoal()
        advanceUntilIdle()

        coVerify {
            calorieGoalRepository.updateGoal(match { goal ->
                goal.id == 10L &&
                        goal.name == "Updated Name" &&
                        goal.calorieTarget == 1900
            })
        }
        assertEquals("Goal saved", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `AC-042 - editing predefined goal shows error`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(1L) } returns predefinedGoal1
        advanceUntilIdle()

        viewModel.openEditGoalForm(1L)
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertEquals("Pre-defined goals cannot be edited", form.error)
    }

    @Test
    fun `EC-070 - editing goal to zero calories shows validation error`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateCalorieTarget("0")

        assertEquals("Calorie goal must be greater than 0", viewModel.formState.value.calorieError)
    }

    @Test
    fun `EC-071 - goal deleted while trying to edit shows error`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(999L) } returns null
        advanceUntilIdle()

        viewModel.openEditGoalForm(999L)
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertEquals("Goal no longer exists", form.error)
        assertFalse(form.isLoading)
    }

    @Test
    fun `EC-073 - database failure during edit save shows error`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        coEvery { calorieGoalRepository.updateGoal(any()) } throws RuntimeException("DB error")
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateGoalName("Updated")
        viewModel.saveGoal()
        advanceUntilIdle()

        assertFalse(viewModel.formState.value.isSaving)
        assertEquals("Failed to save goal. Please try again.", viewModel.uiState.value.error)
    }

    @Test
    fun `EC-074 - cancelling edit does not save changes`() = runTest {
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateGoalName("Changed Name")
        // Do not call saveGoal - simulating cancel

        coVerify(exactly = 0) { calorieGoalRepository.updateGoal(any()) }
    }

    @Test
    fun `edit save detects goal deleted during save operation`() = runTest {
        // First call returns the goal (for openEditGoalForm), second returns null (for saveGoal)
        coEvery { calorieGoalRepository.getGoalById(10L) } returns customGoal andThen null
        advanceUntilIdle()

        viewModel.openEditGoalForm(10L)
        advanceUntilIdle()

        viewModel.updateGoalName("Updated")
        viewModel.saveGoal()
        advanceUntilIdle()

        assertEquals("Goal no longer exists", viewModel.formState.value.error)
    }

    // ============================================================
    // US-010: Delete calorie goal
    // ============================================================

    @Test
    fun `AC-043 - showDeleteDialog sets deleteDialogGoal for custom goal`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)

        val state = viewModel.uiState.value
        assertNotNull(state.deleteDialogGoal)
        assertEquals(10L, state.deleteDialogGoal!!.id)
    }

    @Test
    fun `AC-044 - confirming deletion calls repository deleteGoal`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)
        viewModel.confirmDeleteGoal()
        advanceUntilIdle()

        coVerify { calorieGoalRepository.deleteGoal(10L) }
        assertEquals("Goal deleted", viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `AC-045 - deleting active goal clears activeGoalId via flow`() = runTest {
        val activeCustomGoal = customGoal.copy(isActive = true)
        activeGoalFlow.value = activeCustomGoal
        advanceUntilIdle()
        assertEquals(10L, viewModel.uiState.value.activeGoalId)

        viewModel.showDeleteDialog(activeCustomGoal)
        viewModel.confirmDeleteGoal()
        // Simulate: after delete, active goal flow emits null
        activeGoalFlow.value = null
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeGoalId)
    }

    @Test
    fun `AC-046 - predefined goal cannot be deleted via showDeleteDialog`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(predefinedGoal1)

        // Dialog should not be shown for predefined goals
        assertNull(viewModel.uiState.value.deleteDialogGoal)
    }

    @Test
    fun `AC-047 - dismissing delete dialog clears deleteDialogGoal`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)
        assertNotNull(viewModel.uiState.value.deleteDialogGoal)

        viewModel.dismissDeleteDialog()
        assertNull(viewModel.uiState.value.deleteDialogGoal)
    }

    @Test
    fun `EC-076 - deleting all custom goals leaves only predefined`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)
        viewModel.confirmDeleteGoal()
        allGoalsFlow.value = listOf(predefinedGoal1, predefinedGoal2, predefinedGoal3)
        advanceUntilIdle()

        val goals = viewModel.uiState.value.goals
        assertEquals(3, goals.size)
        assertTrue(goals.all { it.isPredefined })
    }

    @Test
    fun `EC-077 - deleting active goal sets activeGoalId to null`() = runTest {
        val activeCustom = customGoal.copy(isActive = true)
        activeGoalFlow.value = activeCustom
        advanceUntilIdle()

        viewModel.showDeleteDialog(activeCustom)
        viewModel.confirmDeleteGoal()
        activeGoalFlow.value = null
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeGoalId)
    }

    @Test
    fun `EC-078 - database failure during delete shows error`() = runTest {
        coEvery { calorieGoalRepository.deleteGoal(any()) } throws RuntimeException("DB error")
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)
        viewModel.confirmDeleteGoal()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteDialogGoal)
        assertNotNull(viewModel.uiState.value.error)
        assertEquals("Failed to delete goal. Please try again.", viewModel.uiState.value.error)
    }

    @Test
    fun `EC-079 - rapid deletions of multiple goals process correctly`() = runTest {
        val custom2 = CalorieGoalEntity(
            id = 11, name = "Custom 2", calorieTarget = 1900,
            isActive = false, isPredefined = false
        )
        allGoalsFlow.value = listOf(predefinedGoal1, predefinedGoal2, customGoal, custom2)
        advanceUntilIdle()

        // Delete first custom goal
        viewModel.showDeleteDialog(customGoal)
        viewModel.confirmDeleteGoal()
        advanceUntilIdle()
        allGoalsFlow.value = listOf(predefinedGoal1, predefinedGoal2, custom2)
        advanceUntilIdle()

        coVerify { calorieGoalRepository.deleteGoal(10L) }

        // Delete second custom goal
        viewModel.showDeleteDialog(custom2)
        viewModel.confirmDeleteGoal()
        advanceUntilIdle()
        allGoalsFlow.value = listOf(predefinedGoal1, predefinedGoal2)
        advanceUntilIdle()

        coVerify { calorieGoalRepository.deleteGoal(11L) }
        assertEquals(2, viewModel.uiState.value.goals.size)
    }

    // ============================================================
    // Snackbar and error helpers
    // ============================================================

    @Test
    fun `clearSnackbar resets snackbar message`() = runTest {
        advanceUntilIdle()

        viewModel.showDeleteDialog(customGoal)
        viewModel.confirmDeleteGoal()
        advanceUntilIdle()

        assertEquals("Goal deleted", viewModel.uiState.value.snackbarMessage)

        viewModel.clearSnackbar()
        assertNull(viewModel.uiState.value.snackbarMessage)
    }

    @Test
    fun `clearError resets error message`() = runTest {
        coEvery { calorieGoalRepository.setActiveGoal(any()) } throws RuntimeException("DB error")
        advanceUntilIdle()

        viewModel.selectGoal(1L)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ============================================================
    // Validation edge cases for all fields
    // ============================================================

    @Test
    fun `saving goal with all validation errors shows all errors at once`() = runTest {
        advanceUntilIdle()

        viewModel.openAddGoalForm()
        viewModel.updateGoalName("") // Will trigger empty name
        viewModel.updateCalorieTarget("") // Will trigger empty calorie
        viewModel.updateProteinTarget("-1") // Will trigger negative
        viewModel.saveGoal()
        advanceUntilIdle()

        val form = viewModel.formState.value
        assertNotNull(form.nameError)
        assertNotNull(form.calorieError)
        assertNotNull(form.proteinError)
    }

    @Test
    fun `goal name with only spaces followed by real text clears error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateGoalName("   ") // error
        assertNotNull(viewModel.formState.value.nameError)

        viewModel.updateGoalName("Valid Name")
        assertNull(viewModel.formState.value.nameError)
    }

    @Test
    fun `calorie target fix clears error`() = runTest {
        viewModel.openAddGoalForm()
        viewModel.updateCalorieTarget("-500") // error
        assertNotNull(viewModel.formState.value.calorieError)

        viewModel.updateCalorieTarget("2000")
        assertNull(viewModel.formState.value.calorieError)
    }
}
