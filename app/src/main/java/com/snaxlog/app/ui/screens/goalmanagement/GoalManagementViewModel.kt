package com.snaxlog.app.ui.screens.goalmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.repository.CalorieGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * UI state for the Goal Management screen (S-002).
 */
data class GoalManagementUiState(
    val goals: List<CalorieGoalEntity> = emptyList(),
    val activeGoalId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteDialogGoal: CalorieGoalEntity? = null,
    val snackbarMessage: String? = null
)

/**
 * UI state for the Add/Edit Goal bottom sheet (S-005).
 */
data class GoalFormUiState(
    val isEditMode: Boolean = false,
    val editingGoalId: Long? = null,
    val nameInput: String = "",
    val nameError: String? = null,
    val calorieInput: String = "",
    val calorieError: String? = null,
    val proteinInput: String = "",
    val proteinError: String? = null,
    val fatInput: String = "",
    val fatError: String? = null,
    val carbsInput: String = "",
    val carbsError: String? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GoalManagementViewModel @Inject constructor(
    private val calorieGoalRepository: CalorieGoalRepository
) : ViewModel() {

    // Main screen state
    private val _uiState = MutableStateFlow(GoalManagementUiState())
    val uiState: StateFlow<GoalManagementUiState> = _uiState.asStateFlow()

    // Goal form state
    private val _formState = MutableStateFlow(GoalFormUiState())
    val formState: StateFlow<GoalFormUiState> = _formState.asStateFlow()

    init {
        observeGoals()
        observeActiveGoal()
    }

    private fun observeGoals() {
        viewModelScope.launch {
            calorieGoalRepository.getAllGoals().collect { goals ->
                _uiState.update {
                    it.copy(
                        goals = goals,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private fun observeActiveGoal() {
        viewModelScope.launch {
            calorieGoalRepository.getActiveGoal().collect { goal ->
                _uiState.update { it.copy(activeGoalId = goal?.id) }
            }
        }
    }

    // ============================
    // US-007: Select active calorie goal
    // ============================

    fun selectGoal(goalId: Long) {
        viewModelScope.launch {
            try {
                calorieGoalRepository.setActiveGoal(goalId)
                // No snackbar needed for selection, visual indicator is enough
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to set active goal. Please try again.")
                }
            }
        }
    }

    // ============================
    // US-010: Delete calorie goal
    // ============================

    fun showDeleteDialog(goal: CalorieGoalEntity) {
        // AC-046: Pre-defined goals cannot be deleted
        if (goal.isPredefined) return
        _uiState.update { it.copy(deleteDialogGoal = goal) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogGoal = null) }
    }

    fun confirmDeleteGoal() {
        val goal = _uiState.value.deleteDialogGoal ?: return
        viewModelScope.launch {
            try {
                val wasActive = goal.id == _uiState.value.activeGoalId
                calorieGoalRepository.deleteGoal(goal.id)
                _uiState.update {
                    it.copy(
                        deleteDialogGoal = null,
                        snackbarMessage = "Goal deleted"
                    )
                }
                // AC-045: If deleted goal was active, activeGoalId will update
                // via the Flow observer automatically
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        deleteDialogGoal = null,
                        error = "Failed to delete goal. Please try again."
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // ============================
    // US-008: Add custom calorie goal
    // ============================

    fun openAddGoalForm() {
        _formState.value = GoalFormUiState()
    }

    // ============================
    // US-009: Edit calorie goal
    // ============================

    fun openEditGoalForm(goalId: Long) {
        _formState.value = GoalFormUiState(isEditMode = true, isLoading = true)
        viewModelScope.launch {
            try {
                val goal = calorieGoalRepository.getGoalById(goalId)
                if (goal != null) {
                    // AC-042: Pre-defined goals cannot be edited
                    if (goal.isPredefined) {
                        _formState.update {
                            it.copy(isLoading = false, error = "Pre-defined goals cannot be edited")
                        }
                        return@launch
                    }
                    _formState.update {
                        it.copy(
                            isEditMode = true,
                            editingGoalId = goal.id,
                            nameInput = goal.name,
                            calorieInput = goal.calorieTarget.toString(),
                            proteinInput = goal.proteinTarget?.let { p -> formatOptionalDouble(p) } ?: "",
                            fatInput = goal.fatTarget?.let { f -> formatOptionalDouble(f) } ?: "",
                            carbsInput = goal.carbsTarget?.let { c -> formatOptionalDouble(c) } ?: "",
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    // EC-071: Goal deleted while trying to edit
                    _formState.update {
                        it.copy(isLoading = false, error = "Goal no longer exists")
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isLoading = false, error = "Failed to load goal")
                }
            }
        }
    }

    // ============================
    // Form input handlers
    // ============================

    fun updateGoalName(name: String) {
        // EC-063: Limit goal name to 50 characters
        val limited = name.take(50)
        val error = validateGoalName(limited)
        _formState.update { it.copy(nameInput = limited, nameError = error) }
    }

    fun updateCalorieTarget(input: String) {
        val error = validateCalorieTarget(input)
        _formState.update { it.copy(calorieInput = input, calorieError = error) }
    }

    fun updateProteinTarget(input: String) {
        val error = validateOptionalMacro(input, "Protein")
        _formState.update { it.copy(proteinInput = input, proteinError = error) }
    }

    fun updateFatTarget(input: String) {
        val error = validateOptionalMacro(input, "Fat")
        _formState.update { it.copy(fatInput = input, fatError = error) }
    }

    fun updateCarbsTarget(input: String) {
        val error = validateOptionalMacro(input, "Carbs")
        _formState.update { it.copy(carbsInput = input, carbsError = error) }
    }

    // ============================
    // Save goal
    // ============================

    fun saveGoal() {
        val state = _formState.value

        // Validate all fields
        val nameError = validateGoalName(state.nameInput)
        val calorieError = validateCalorieTarget(state.calorieInput)
        val proteinError = validateOptionalMacro(state.proteinInput, "Protein")
        val fatError = validateOptionalMacro(state.fatInput, "Fat")
        val carbsError = validateOptionalMacro(state.carbsInput, "Carbs")

        if (nameError != null || calorieError != null || proteinError != null ||
            fatError != null || carbsError != null
        ) {
            _formState.update {
                it.copy(
                    nameError = nameError,
                    calorieError = calorieError,
                    proteinError = proteinError,
                    fatError = fatError,
                    carbsError = carbsError
                )
            }
            return
        }

        // EC-020 equivalent: Prevent duplicate saves
        if (state.isSaving) return
        _formState.update { it.copy(isSaving = true) }

        val calorieTarget = state.calorieInput.toIntOrNull()
            ?: state.calorieInput.toDoubleOrNull()?.roundToInt()
            ?: return

        val proteinTarget = state.proteinInput.toDoubleOrNull()
        val fatTarget = state.fatInput.toDoubleOrNull()
        val carbsTarget = state.carbsInput.toDoubleOrNull()

        viewModelScope.launch {
            try {
                if (state.isEditMode && state.editingGoalId != null) {
                    // Update existing goal
                    val existingGoal = calorieGoalRepository.getGoalById(state.editingGoalId)
                    if (existingGoal == null) {
                        _formState.update {
                            it.copy(isSaving = false, error = "Goal no longer exists")
                        }
                        return@launch
                    }
                    val updatedGoal = existingGoal.copy(
                        name = state.nameInput.trim(),
                        calorieTarget = calorieTarget,
                        proteinTarget = proteinTarget,
                        fatTarget = fatTarget,
                        carbsTarget = carbsTarget
                    )
                    calorieGoalRepository.updateGoal(updatedGoal)
                    _formState.update { it.copy(isSaving = false) }
                    _uiState.update { it.copy(snackbarMessage = "Goal saved") }
                } else {
                    // Create new goal
                    val newGoal = CalorieGoalEntity(
                        name = state.nameInput.trim(),
                        calorieTarget = calorieTarget,
                        proteinTarget = proteinTarget,
                        fatTarget = fatTarget,
                        carbsTarget = carbsTarget,
                        isActive = false,
                        isPredefined = false
                    )
                    calorieGoalRepository.addGoal(newGoal)
                    _formState.update { it.copy(isSaving = false) }
                    _uiState.update { it.copy(snackbarMessage = "Goal saved") }
                }
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(error = "Failed to save goal. Please try again.") }
            }
        }
    }

    // ============================
    // Validation
    // ============================

    private fun validateGoalName(name: String): String? {
        // AC-036, EC-062: Goal name cannot be empty or whitespace
        if (name.isBlank()) return "Goal name cannot be empty"
        return null
    }

    private fun validateCalorieTarget(input: String): String? {
        if (input.isBlank()) return "Calorie target is required"

        val value = input.toDoubleOrNull()
            ?: return "Please enter a valid number"

        // EC-057: Zero calories
        // EC-058: Negative calories
        if (value <= 0) return "Calorie goal must be greater than 0"

        return null
    }

    private fun validateOptionalMacro(input: String, name: String): String? {
        if (input.isBlank()) return null // Optional field

        val value = input.toDoubleOrNull()
            ?: return "Please enter a valid number"

        if (value < 0) return "$name target cannot be negative"

        return null
    }

    // ============================
    // Helpers
    // ============================

    private fun formatOptionalDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
