package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.data.repository.FoodIntakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * UI state for the Edit Food bottom sheet (S-004).
 * FIP-005: Added meal category field.
 * FIP-EPIC-005: Added entry date display for historical context (US-015).
 *
 * [saveSuccess] is a one-shot signal that the edit was persisted; the screen consumes it to
 * close the sheet and show the "Entry updated" snackbar, then calls [EditFoodViewModel.onSaveHandled].
 * [saveError] signals a save failure that the screen surfaces to the user.
 */
data class EditFoodUiState(
    val entry: FoodIntakeWithFood? = null,
    val servingsInput: String = "1.0",
    val servingsError: String? = null,
    val previewCalories: Int = 0,
    val previewProtein: Double = 0.0,
    val previewFat: Double = 0.0,
    val previewCarbs: Double = 0.0,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    // FIP-005: Meal category field (no auto-selection in edit mode)
    val selectedCategory: MealCategory? = null,
    // FIP-EPIC-005: Entry date for context display
    val entryDate: LocalDate? = null,
    val isEditingHistorical: Boolean = false,
    // One-shot result signals consumed by the screen
    val saveSuccess: Boolean = false,
    val saveError: Boolean = false
)

/**
 * ViewModel backing the Edit Food bottom sheet (S-004), split out of DailyFoodLogViewModel.
 *
 * Owns loading the entry for edit, serving-size entry/validation, the nutrition preview, and
 * persisting the update. Preserves the injected [Clock] pattern used to detect whether an
 * entry belongs to a historical day (US-015).
 */
@HiltViewModel
class EditFoodViewModel @Inject constructor(
    private val foodIntakeRepository: FoodIntakeRepository,
    private val clock: Clock
) : ViewModel() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val _editFoodState = MutableStateFlow(EditFoodUiState())
    val editFoodState: StateFlow<EditFoodUiState> = _editFoodState.asStateFlow()

    fun loadEntryForEdit(entryId: Long) {
        _editFoodState.value = EditFoodUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val entryWithFood = foodIntakeRepository.getEntryWithFoodById(entryId)
                if (entryWithFood != null) {
                    val servingsStr = formatServings(entryWithFood.entry.servings)
                    // FIP-EPIC-005 US-015: Parse entry date for historical context
                    val entryDate = try {
                        LocalDate.parse(entryWithFood.entry.date, DATE_FORMATTER)
                    } catch (e: Exception) {
                        null
                    }
                    val today = LocalDate.now(clock)
                    val isHistorical = entryDate != null && entryDate != today

                    // FIP-005: Load existing meal category
                    _editFoodState.update {
                        it.copy(
                            entry = entryWithFood,
                            servingsInput = servingsStr,
                            previewCalories = entryWithFood.entry.totalCalories,
                            previewProtein = entryWithFood.entry.totalProtein,
                            previewFat = entryWithFood.entry.totalFat,
                            previewCarbs = entryWithFood.entry.totalCarbs,
                            isLoading = false,
                            error = null,
                            selectedCategory = entryWithFood.entry.mealCategory,
                            entryDate = entryDate,
                            isEditingHistorical = isHistorical
                        )
                    }
                } else {
                    // EC-024: Entry was deleted while user is trying to edit
                    _editFoodState.update {
                        it.copy(isLoading = false, error = "Entry no longer exists")
                    }
                }
            } catch (e: Exception) {
                _editFoodState.update {
                    it.copy(isLoading = false, error = "Failed to load entry")
                }
            }
        }
    }

    /**
     * FIP-005: Update meal category selection in edit food flow.
     */
    fun updateEditFoodCategory(category: MealCategory?) {
        _editFoodState.update { it.copy(selectedCategory = category) }
    }

    fun updateEditFoodServings(input: String) {
        val entryWithFood = _editFoodState.value.entry ?: return
        val error = validateServings(input)
        _editFoodState.update { it.copy(servingsInput = input, servingsError = error) }
        if (error == null) {
            val servings = input.toDoubleOrNull() ?: return
            if (servings <= 0) return
            val food = entryWithFood.food
            _editFoodState.update {
                it.copy(
                    previewCalories = (food.caloriesPerServing * servings).roundToInt(),
                    previewProtein = roundToOneDecimal(food.proteinPerServing * servings),
                    previewFat = roundToOneDecimal(food.fatPerServing * servings),
                    previewCarbs = roundToOneDecimal(food.carbsPerServing * servings)
                )
            }
        }
    }

    fun saveEditFood() {
        val state = _editFoodState.value
        val entryWithFood = state.entry ?: return
        val servings = state.servingsInput.toDoubleOrNull() ?: return
        val error = validateServings(state.servingsInput)
        if (error != null) {
            _editFoodState.update { it.copy(servingsError = error) }
            return
        }
        if (state.isSaving) return
        _editFoodState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val food = entryWithFood.food
                // FIP-005: Include meal category in update
                val updatedEntry = entryWithFood.entry.copy(
                    servings = servings,
                    totalCalories = (food.caloriesPerServing * servings).roundToInt(),
                    totalProtein = roundToOneDecimal(food.proteinPerServing * servings),
                    totalFat = roundToOneDecimal(food.fatPerServing * servings),
                    totalCarbs = roundToOneDecimal(food.carbsPerServing * servings),
                    mealCategory = state.selectedCategory
                )
                foodIntakeRepository.updateEntry(updatedEntry)
                _editFoodState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _editFoodState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    /**
     * Consumes the one-shot [EditFoodUiState.saveSuccess] / [EditFoodUiState.saveError] signals
     * after the screen has handled them (closed the sheet, shown a snackbar).
     */
    fun onSaveHandled() {
        _editFoodState.update { it.copy(saveSuccess = false, saveError = false) }
    }

    // ============================
    // Validation (EC-011..EC-015, EC-023)
    // ============================

    private fun validateServings(input: String): String? {
        if (input.isBlank()) return "Serving size is required"

        val servings = input.toDoubleOrNull()
            ?: return "Please enter a valid number"

        if (servings <= 0) return "Serving size must be greater than 0"

        // EC-014: Check decimal places
        if (input.contains(".") && input.substringAfter(".").length > 2) {
            return "Maximum 2 decimal places"
        }

        return null
    }

    private fun roundToOneDecimal(value: Double): Double {
        return (value * 10).roundToInt() / 10.0
    }

    private fun formatServings(servings: Double): String {
        return if (servings == servings.toLong().toDouble()) {
            "${servings.toLong()}.0"
        } else {
            String.format("%.2f", servings).trimEnd('0').let {
                if (it.endsWith(".")) "${it}0" else it
            }
        }
    }
}
