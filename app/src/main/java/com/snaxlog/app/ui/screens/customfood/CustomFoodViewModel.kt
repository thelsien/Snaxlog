package com.snaxlog.app.ui.screens.customfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.snaxlog.app.R
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.data.repository.FoodRepository
import com.snaxlog.app.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Create/Edit Custom Food form.
 * EPIC-006: US-018, US-021
 */
data class CustomFoodFormUiState(
    // Mode
    val isEditMode: Boolean = false,
    val editingFoodId: Long? = null,

    // Form inputs
    val nameInput: String = "",
    val nameError: UiText? = null,

    val servingSizeInput: String = "",
    val servingSizeError: UiText? = null,

    val servingUnit: ServingUnit = ServingUnit.GRAM,

    val proteinInput: String = "",
    val proteinError: UiText? = null,

    val fatInput: String = "",
    val fatError: UiText? = null,

    val carbsInput: String = "",
    val carbsError: UiText? = null,

    // Calculated values
    val calculatedCalories: Int = 0,

    // State
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val duplicateNameWarning: Boolean = false,
    val saveSuccess: Boolean = false
)

/**
 * UI state for the Custom Food list screen.
 * EPIC-006: US-020
 */
data class CustomFoodListUiState(
    val foods: List<FoodEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val deleteDialogFood: FoodEntity? = null,
    val deleteWarningMessage: UiText? = null,
    val snackbarMessage: UiText? = null
)

/**
 * ViewModel for Custom Food operations.
 * EPIC-006: User-Created Foods and Recipes
 * US-018: Create Simple Custom Food
 * US-020: View and Use Custom Foods
 * US-021: Edit Custom Foods
 * US-022: Delete Custom Foods
 */
@HiltViewModel
class CustomFoodViewModel @Inject constructor(
    private val foodRepository: FoodRepository
) : ViewModel() {

    // Form state for create/edit
    private val _formState = MutableStateFlow(CustomFoodFormUiState())
    val formState: StateFlow<CustomFoodFormUiState> = _formState.asStateFlow()

    // List state for viewing custom foods
    private val _listState = MutableStateFlow(CustomFoodListUiState())
    val listState: StateFlow<CustomFoodListUiState> = _listState.asStateFlow()

    init {
        observeCustomFoods()
    }

    private fun observeCustomFoods() {
        viewModelScope.launch {
            foodRepository.getAllUserCreatedFoods().collect { foods ->
                _listState.update {
                    it.copy(
                        foods = foods,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    // ============================
    // US-018: Create Simple Custom Food
    // ============================

    /**
     * Initialize form for creating a new custom food.
     */
    fun openCreateForm() {
        _formState.value = CustomFoodFormUiState()
    }

    // ============================
    // US-021: Edit Custom Food
    // ============================

    /**
     * Initialize form for editing an existing custom food.
     * AC-016-001: Pre-fills form with existing values.
     */
    fun openEditForm(foodId: Long) {
        _formState.value = CustomFoodFormUiState(isEditMode = true, isLoading = true)
        viewModelScope.launch {
            try {
                val food = foodRepository.getFoodById(foodId)
                if (food != null) {
                    // Verify it's editable
                    if (!food.isEditable) {
                        _formState.update {
                            it.copy(isLoading = false, error = UiText.StringResource(R.string.custom_food_error_not_editable))
                        }
                        return@launch
                    }
                    // Verify it's not a recipe (use recipe editor for recipes)
                    if (food.foodType == FoodType.RECIPE) {
                        _formState.update {
                            it.copy(isLoading = false, error = UiText.StringResource(R.string.custom_food_error_use_recipe_editor))
                        }
                        return@launch
                    }

                    _formState.update {
                        it.copy(
                            isEditMode = true,
                            editingFoodId = food.id,
                            nameInput = food.name,
                            servingSizeInput = formatDouble(food.servingSizeValue),
                            servingUnit = food.servingUnit,
                            proteinInput = formatDouble(food.proteinPerServing),
                            fatInput = formatDouble(food.fatPerServing),
                            carbsInput = formatDouble(food.carbsPerServing),
                            calculatedCalories = food.caloriesPerServing,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _formState.update {
                        it.copy(isLoading = false, error = UiText.StringResource(R.string.custom_food_error_not_found))
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isLoading = false, error = UiText.StringResource(R.string.custom_food_error_load_failed))
                }
            }
        }
    }

    // ============================
    // Form input handlers
    // ============================

    /**
     * Update food name input.
     * EC-013-005: Limit to 100 characters.
     * EC-013-007: Check for duplicate names.
     */
    fun updateName(name: String) {
        // EC-013-005: Limit to 100 characters
        val limited = name.take(100)
        val error = validateName(limited)
        _formState.update { it.copy(nameInput = limited, nameError = error) }

        // Check for duplicate name (async)
        if (error == null && limited.isNotBlank()) {
            checkDuplicateName(limited)
        } else {
            _formState.update { it.copy(duplicateNameWarning = false) }
        }
    }

    private fun checkDuplicateName(name: String) {
        viewModelScope.launch {
            val exists = foodRepository.foodNameExists(name)
            // Don't show warning if editing and it's the same name
            val isOwnName = _formState.value.isEditMode &&
                    _formState.value.editingFoodId != null &&
                    name.trim().equals(_formState.value.nameInput.trim(), ignoreCase = true)

            _formState.update {
                it.copy(duplicateNameWarning = exists && !isOwnName)
            }
        }
    }

    /**
     * Update serving size input.
     * EC-013-003: Must be greater than 0.
     */
    fun updateServingSize(input: String) {
        val filtered = filterNumericInput(input)
        val error = validateServingSize(filtered)
        _formState.update { it.copy(servingSizeInput = filtered, servingSizeError = error) }
    }

    /**
     * Update serving unit selection.
     */
    fun updateServingUnit(unit: ServingUnit) {
        _formState.update { it.copy(servingUnit = unit) }
    }

    /**
     * Update protein input.
     * EC-013-002: Must be >= 0.
     */
    fun updateProtein(input: String) {
        val filtered = filterNumericInput(input)
        val error = validateMacro(filtered, R.string.custom_food_macro_protein)
        _formState.update { it.copy(proteinInput = filtered, proteinError = error) }
        recalculateCalories()
    }

    /**
     * Update fat input.
     * EC-013-002: Must be >= 0.
     */
    fun updateFat(input: String) {
        val filtered = filterNumericInput(input)
        val error = validateMacro(filtered, R.string.custom_food_macro_fat)
        _formState.update { it.copy(fatInput = filtered, fatError = error) }
        recalculateCalories()
    }

    /**
     * Update carbs input.
     * EC-013-002: Must be >= 0.
     */
    fun updateCarbs(input: String) {
        val filtered = filterNumericInput(input)
        val error = validateMacro(filtered, R.string.custom_food_macro_carbs)
        _formState.update { it.copy(carbsInput = filtered, carbsError = error) }
        recalculateCalories()
    }

    /**
     * Recalculate calories based on current macro inputs.
     * AC-013-002: Calories = (Protein x 4) + (Carbs x 4) + (Fat x 9)
     */
    private fun recalculateCalories() {
        val protein = _formState.value.proteinInput.toDoubleOrNull() ?: 0.0
        val fat = _formState.value.fatInput.toDoubleOrNull() ?: 0.0
        val carbs = _formState.value.carbsInput.toDoubleOrNull() ?: 0.0

        val calories = FoodEntity.calculateCalories(protein, fat, carbs)
        _formState.update { it.copy(calculatedCalories = calories) }
    }

    // ============================
    // Save custom food
    // ============================

    /**
     * Save the custom food (create or update).
     * EC-013-001: Validates all inputs before saving.
     * EC-013-004: Prevents duplicate submissions.
     */
    fun saveCustomFood() {
        val state = _formState.value

        // Validate all fields
        val nameError = validateName(state.nameInput)
        val servingSizeError = validateServingSize(state.servingSizeInput)
        val proteinError = validateMacro(state.proteinInput, R.string.custom_food_macro_protein)
        val fatError = validateMacro(state.fatInput, R.string.custom_food_macro_fat)
        val carbsError = validateMacro(state.carbsInput, R.string.custom_food_macro_carbs)

        if (nameError != null || servingSizeError != null ||
            proteinError != null || fatError != null || carbsError != null
        ) {
            _formState.update {
                it.copy(
                    nameError = nameError,
                    servingSizeError = servingSizeError,
                    proteinError = proteinError,
                    fatError = fatError,
                    carbsError = carbsError
                )
            }
            return
        }

        // EC-013-004: Prevent duplicate submissions
        if (state.isSaving) return
        _formState.update { it.copy(isSaving = true) }

        val servingSizeValue = state.servingSizeInput.toDoubleOrNull() ?: return
        val protein = state.proteinInput.toDoubleOrNull() ?: 0.0
        val fat = state.fatInput.toDoubleOrNull() ?: 0.0
        val carbs = state.carbsInput.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            try {
                if (state.isEditMode && state.editingFoodId != null) {
                    // Update existing food
                    foodRepository.updateCustomFood(
                        foodId = state.editingFoodId,
                        name = state.nameInput.trim(),
                        servingSizeValue = servingSizeValue,
                        servingUnit = state.servingUnit,
                        protein = protein,
                        fat = fat,
                        carbs = carbs
                    )
                    _formState.update {
                        it.copy(isSaving = false, saveSuccess = true)
                    }
                    _listState.update {
                        it.copy(snackbarMessage = UiText.StringResource(R.string.custom_food_msg_updated))
                    }
                } else {
                    // Create new food
                    foodRepository.createCustomFood(
                        name = state.nameInput.trim(),
                        servingSizeValue = servingSizeValue,
                        servingUnit = state.servingUnit,
                        protein = protein,
                        fat = fat,
                        carbs = carbs
                    )
                    _formState.update {
                        it.copy(isSaving = false, saveSuccess = true)
                    }
                    _listState.update {
                        it.copy(snackbarMessage = UiText.StringResource(R.string.custom_food_msg_created))
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isSaving = false, error = UiText.StringResource(R.string.custom_food_error_save_failed))
                }
            }
        }
    }

    // ============================
    // US-022: Delete Custom Food
    // ============================

    /**
     * Show delete confirmation dialog.
     * EC-016-001: Only user-created foods can be deleted.
     * EC-016-002, EC-016-003: Shows usage warnings.
     */
    fun showDeleteDialog(food: FoodEntity) {
        if (!food.isDeletable) return

        viewModelScope.launch {
            val intakeCount = foodRepository.getIntakeUsageCount(food.id)
            val recipeCount = foodRepository.getRecipeUsageCount(food.id)

            val warningMessage = buildDeleteWarningMessage(intakeCount, recipeCount)

            _listState.update {
                it.copy(
                    deleteDialogFood = food,
                    deleteWarningMessage = warningMessage
                )
            }
        }
    }

    private fun buildDeleteWarningMessage(intakeCount: Int, recipeCount: Int): UiText? {
        // Build positional count fragments (e.g. "2 log entries") so the full warning sentence
        // is assembled from string resources without string concatenation in code.
        val intakeFragment = if (intakeCount > 0) {
            // EC-016-002: Food is used in intake logs
            UiText.StringResource(
                if (intakeCount == 1) R.string.custom_food_delete_fragment_intake_one
                else R.string.custom_food_delete_fragment_intake_other,
                listOf(intakeCount)
            )
        } else {
            null
        }
        val recipeFragment = if (recipeCount > 0) {
            // EC-016-003: Food is used in recipes
            UiText.StringResource(
                if (recipeCount == 1) R.string.custom_food_delete_fragment_recipe_one
                else R.string.custom_food_delete_fragment_recipe_other,
                listOf(recipeCount)
            )
        } else {
            null
        }

        return when {
            intakeFragment != null && recipeFragment != null -> UiText.StringResource(
                R.string.custom_food_delete_warning_both,
                listOf(intakeFragment, recipeFragment)
            )
            intakeFragment != null -> UiText.StringResource(
                R.string.custom_food_delete_warning_intake,
                listOf(intakeFragment)
            )
            recipeFragment != null -> UiText.StringResource(
                R.string.custom_food_delete_warning_recipe,
                listOf(recipeFragment)
            )
            else -> null
        }
    }

    /**
     * Dismiss delete dialog.
     */
    fun dismissDeleteDialog() {
        _listState.update {
            it.copy(deleteDialogFood = null, deleteWarningMessage = null)
        }
    }

    /**
     * Confirm deletion of custom food.
     * AC-017-001: Permanent deletion after confirmation.
     */
    fun confirmDeleteFood() {
        val food = _listState.value.deleteDialogFood ?: return

        viewModelScope.launch {
            try {
                foodRepository.deleteCustomFood(food.id)
                _listState.update {
                    it.copy(
                        deleteDialogFood = null,
                        deleteWarningMessage = null,
                        snackbarMessage = UiText.StringResource(R.string.custom_food_msg_deleted)
                    )
                }
            } catch (e: Exception) {
                _listState.update {
                    it.copy(
                        deleteDialogFood = null,
                        deleteWarningMessage = null,
                        error = UiText.StringResource(R.string.custom_food_error_delete_failed)
                    )
                }
            }
        }
    }

    // ============================
    // Utility functions
    // ============================

    fun clearSnackbar() {
        _listState.update { it.copy(snackbarMessage = null) }
    }

    fun clearError() {
        _formState.update { it.copy(error = null) }
        _listState.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _formState.update { it.copy(saveSuccess = false) }
    }

    // ============================
    // Validation helpers
    // ============================

    private fun validateName(name: String): UiText? {
        // EC-013-001: Name is required
        if (name.isBlank()) return UiText.StringResource(R.string.custom_food_error_name_required)
        return null
    }

    private fun validateServingSize(input: String): UiText? {
        // EC-013-001: Serving size is required
        if (input.isBlank()) return UiText.StringResource(R.string.custom_food_error_serving_required)

        val value = input.toDoubleOrNull()
            ?: return UiText.StringResource(R.string.custom_food_error_invalid_number)

        // EC-013-003: Must be greater than 0
        if (value <= 0) return UiText.StringResource(R.string.custom_food_error_serving_positive)

        return null
    }

    private fun validateMacro(input: String, @StringRes nameRes: Int): UiText? {
        // EC-013-001: Macros are required
        if (input.isBlank()) {
            return UiText.StringResource(
                R.string.custom_food_error_macro_required,
                listOf(UiText.StringResource(nameRes))
            )
        }

        val value = input.toDoubleOrNull()
            ?: return UiText.StringResource(R.string.custom_food_error_invalid_number)

        // EC-013-002: Cannot be negative
        if (value < 0) {
            return UiText.StringResource(
                R.string.custom_food_error_macro_negative,
                listOf(UiText.StringResource(nameRes))
            )
        }

        return null
    }

    /**
     * Filter input to only allow valid numeric characters.
     */
    private fun filterNumericInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        // Ensure only one decimal point
        return if (filtered.count { it == '.' } > 1) {
            val firstDot = filtered.indexOf('.')
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        } else {
            filtered
        }
    }

    private fun formatDouble(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format("%.1f", value)
        }
    }
}
