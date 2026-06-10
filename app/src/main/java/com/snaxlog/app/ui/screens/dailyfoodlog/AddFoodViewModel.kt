package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.R
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import com.snaxlog.app.ui.common.UiText
import com.snaxlog.app.util.MealCategoryUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * UI state for the Add Food bottom sheet (S-003).
 * FIP-005: Added meal category fields.
 * FIP-EPIC-005: Added target date for historical entries (US-017).
 *
 * [saveSuccess] is a one-shot signal that an entry was saved successfully; the screen
 * consumes it to close the sheet and show the "Entry added" snackbar, then calls
 * [AddFoodViewModel.onSaveHandled].
 * [saveError] signals a save failure that the screen surfaces to the user.
 */
data class AddFoodUiState(
    val searchQuery: String = "",
    val foods: List<FoodEntity> = emptyList(),
    val selectedFood: FoodEntity? = null,
    val servingsInput: String = "1.0",
    val servingsError: UiText? = null,
    val previewCalories: Int = 0,
    val previewProtein: Double = 0.0,
    val previewFat: Double = 0.0,
    val previewCarbs: Double = 0.0,
    val isSaving: Boolean = false,
    val isLoadingFoods: Boolean = true,
    // FIP-005: Meal category fields
    val selectedCategory: MealCategory? = null,
    val autoSelectedCategory: MealCategory? = null,
    // FIP-EPIC-005: Target date for historical entries
    val targetDate: LocalDate = LocalDate.now(),
    val isAddingToHistorical: Boolean = false,
    // One-shot result signals consumed by the screen
    val saveSuccess: Boolean = false,
    val saveError: Boolean = false
)

/**
 * ViewModel backing the Add Food bottom sheet (S-003), split out of DailyFoodLogViewModel.
 *
 * Owns food search (with debounce), serving-size entry/validation, the nutrition preview,
 * and persisting the new entry. Preserves the injected [Clock] pattern and the
 * selected-date interplay: callers pass the currently selected date via [openAddFood] so
 * entries are logged to the correct (possibly historical) day (US-017).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val foodIntakeRepository: FoodIntakeRepository,
    private val foodRepository: FoodRepository,
    private val clock: Clock
) : ViewModel() {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val _addFoodState = MutableStateFlow(AddFoodUiState())
    val addFoodState: StateFlow<AddFoodUiState> = _addFoodState.asStateFlow()

    // Search query for debounce
    private val _searchQuery = MutableStateFlow("")

    init {
        observeFoodSearch()
    }

    private fun observeFoodSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        foodRepository.getAllFoods()
                    } else {
                        foodRepository.searchFoods(query.trim())
                    }
                }
                .collect { foods ->
                    _addFoodState.update {
                        it.copy(foods = foods, isLoadingFoods = false)
                    }
                }
        }
    }

    /**
     * Initializes the add-food flow for the given [targetDate] (the currently selected date
     * in the daily log). For historical dates, meal-category auto-selection is disabled
     * (FIP-EPIC-005 US-017).
     */
    fun openAddFood(targetDate: LocalDate = LocalDate.now(clock)) {
        val today = LocalDate.now(clock)
        val isHistorical = targetDate != today

        // FIP-005: Auto-select category based on current time
        // FIP-EPIC-005 US-017: Disable auto-selection for historical dates
        val autoCategory = if (isHistorical) null else MealCategoryUtils.getCurrentMealCategory()

        _addFoodState.value = AddFoodUiState(
            selectedCategory = autoCategory,
            autoSelectedCategory = autoCategory,
            targetDate = targetDate,
            isAddingToHistorical = isHistorical
        )
        _searchQuery.value = ""
    }

    /**
     * FIP-005: Update meal category selection in add food flow.
     */
    fun updateAddFoodCategory(category: MealCategory?) {
        _addFoodState.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        // EC-040: Limit search input to 100 characters
        val limited = query.take(100)
        _addFoodState.update { it.copy(searchQuery = limited) }
        _searchQuery.value = limited
    }

    fun clearSearch() {
        _addFoodState.update { it.copy(searchQuery = "") }
        _searchQuery.value = ""
    }

    fun selectFood(food: FoodEntity) {
        _addFoodState.update {
            it.copy(
                selectedFood = food,
                servingsInput = "1.0",
                servingsError = null
            )
        }
        updateAddFoodPreview("1.0", food)
    }

    fun clearFoodSelection() {
        _addFoodState.update {
            it.copy(
                selectedFood = null,
                servingsInput = "1.0",
                servingsError = null,
                previewCalories = 0,
                previewProtein = 0.0,
                previewFat = 0.0,
                previewCarbs = 0.0
            )
        }
    }

    fun updateAddFoodServings(input: String) {
        val food = _addFoodState.value.selectedFood ?: return
        val error = validateServings(input)
        _addFoodState.update { it.copy(servingsInput = input, servingsError = error) }
        if (error == null) {
            updateAddFoodPreview(input, food)
        }
    }

    private fun updateAddFoodPreview(servingsStr: String, food: FoodEntity) {
        val servings = servingsStr.toDoubleOrNull() ?: return
        if (servings <= 0) return
        _addFoodState.update {
            it.copy(
                previewCalories = (food.caloriesPerServing * servings).roundToInt(),
                previewProtein = roundToOneDecimal(food.proteinPerServing * servings),
                previewFat = roundToOneDecimal(food.fatPerServing * servings),
                previewCarbs = roundToOneDecimal(food.carbsPerServing * servings)
            )
        }
    }

    fun saveAddFood() {
        val state = _addFoodState.value
        val food = state.selectedFood ?: return
        val servings = state.servingsInput.toDoubleOrNull() ?: return
        val error = validateServings(state.servingsInput)
        if (error != null) {
            _addFoodState.update { it.copy(servingsError = error) }
            return
        }

        // EC-020: Prevent duplicate saves
        if (state.isSaving) return
        _addFoodState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                // FIP-EPIC-005 US-017: Use target date for historical entries (EC-122)
                val targetDate = state.targetDate
                val today = LocalDate.now(clock)

                // EC-123: Defensive check to prevent future dates
                val safeDate = if (targetDate > today) today else targetDate
                val dateString = safeDate.format(DATE_FORMATTER)

                // For historical entries, use a timestamp at the end of that day
                // For today's entries, use current timestamp
                val timestamp = if (safeDate == today) {
                    clock.millis()
                } else {
                    // Use noon on the historical date for ordering purposes
                    safeDate.atTime(12, 0).toEpochSecond(java.time.ZoneOffset.UTC) * 1000
                }

                // FIP-005: Include meal category in entry
                val entry = FoodIntakeEntryEntity(
                    foodId = food.id,
                    servings = servings,
                    totalCalories = (food.caloriesPerServing * servings).roundToInt(),
                    totalProtein = roundToOneDecimal(food.proteinPerServing * servings),
                    totalFat = roundToOneDecimal(food.fatPerServing * servings),
                    totalCarbs = roundToOneDecimal(food.carbsPerServing * servings),
                    date = dateString,
                    timestamp = timestamp,
                    mealCategory = state.selectedCategory
                )
                foodIntakeRepository.addEntry(entry)
                _addFoodState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _addFoodState.update { it.copy(isSaving = false, saveError = true) }
            }
        }
    }

    /**
     * Consumes the one-shot [AddFoodUiState.saveSuccess] / [AddFoodUiState.saveError] signals
     * after the screen has handled them (closed the sheet, shown a snackbar).
     */
    fun onSaveHandled() {
        _addFoodState.update { it.copy(saveSuccess = false, saveError = false) }
    }

    // ============================
    // Validation (EC-011..EC-015, EC-023)
    // ============================

    private fun validateServings(input: String): UiText? {
        if (input.isBlank()) return UiText.StringResource(R.string.food_entry_error_serving_required)

        val servings = input.toDoubleOrNull()
            ?: return UiText.StringResource(R.string.food_entry_error_invalid_number)

        if (servings <= 0) return UiText.StringResource(R.string.food_entry_error_serving_positive)

        // EC-014: Check decimal places
        if (input.contains(".") && input.substringAfter(".").length > 2) {
            return UiText.StringResource(R.string.food_entry_error_max_decimals)
        }

        return null
    }

    private fun roundToOneDecimal(value: Double): Double {
        return (value * 10).roundToInt() / 10.0
    }
}
