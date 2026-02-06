package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Represents the progress state for a single nutrient (calories or macro).
 * Used to drive visual progress indicators per US-012.
 *
 * @param consumed The amount consumed.
 * @param goal The goal amount, null if no goal is set.
 * @param progress The progress as a float (0.0 = 0%, 1.0 = 100%), null if no goal.
 * @param remaining The remaining amount to reach the goal, null if no goal.
 * @param progressLevel The categorized progress level for UI theming.
 */
data class NutrientProgress(
    val consumed: Double = 0.0,
    val goal: Double? = null,
    val progress: Float? = null,
    val remaining: Double? = null,
    val progressLevel: ProgressLevel = ProgressLevel.NORMAL
)

/**
 * Categorizes progress into levels that drive visual theming (C-004 ProgressBar variants).
 * - NORMAL: 0-89% of goal -> success (green)
 * - APPROACHING: 90-100% of goal -> warning (orange)
 * - EXCEEDED: 100%+ of goal -> error (red)
 * - NO_GOAL: No goal set -> no progress indicator
 */
enum class ProgressLevel {
    NORMAL,
    APPROACHING,
    EXCEEDED,
    NO_GOAL
}

/**
 * UI state for the Daily Food Log screen (S-001).
 *
 * Includes progress states for calories and macros per US-012:
 * - [calorieProgress]: Progress toward calorie goal with threshold-based levels.
 * - [proteinProgress]: Progress toward protein goal.
 * - [fatProgress]: Progress toward fat goal.
 * - [carbsProgress]: Progress toward carbs goal.
 *
 * FIP-EPIC-005: Historical Day Viewing (US-013 to US-017)
 * - [selectedDate]: Currently viewed date (defaults to today)
 * - [isViewingToday]: Computed flag indicating if viewing current day
 * - [isDatePickerOpen]: Whether the date picker dialog is open
 * - [canNavigateForward]: Whether forward navigation is allowed (false when on today)
 */
data class DailyFoodLogUiState(
    val entries: List<FoodIntakeWithFood> = emptyList(),
    val activeGoal: CalorieGoalEntity? = null,
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val calorieProgress: NutrientProgress = NutrientProgress(),
    val proteinProgress: NutrientProgress = NutrientProgress(),
    val fatProgress: NutrientProgress = NutrientProgress(),
    val carbsProgress: NutrientProgress = NutrientProgress(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteDialogEntry: FoodIntakeWithFood? = null,
    val snackbarMessage: String? = null,
    // FIP-EPIC-005: Historical Day Viewing fields
    val selectedDate: LocalDate = LocalDate.now(),
    val isViewingToday: Boolean = true,
    val isDatePickerOpen: Boolean = false,
    val canNavigateForward: Boolean = false
)

/**
 * UI state for the Add Food bottom sheet (S-003).
 * FIP-005: Added meal category fields.
 * FIP-EPIC-005: Added target date for historical entries (US-017).
 */
data class AddFoodUiState(
    val searchQuery: String = "",
    val foods: List<FoodEntity> = emptyList(),
    val selectedFood: FoodEntity? = null,
    val servingsInput: String = "1.0",
    val servingsError: String? = null,
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
    val isAddingToHistorical: Boolean = false
)

/**
 * UI state for the Edit Food bottom sheet (S-004).
 * FIP-005: Added meal category field.
 * FIP-EPIC-005: Added entry date display for historical context (US-015).
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
    val isEditingHistorical: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DailyFoodLogViewModel @Inject constructor(
    private val foodIntakeRepository: FoodIntakeRepository,
    private val foodRepository: FoodRepository,
    private val calorieGoalRepository: CalorieGoalRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // FIP-EPIC-005: Use LocalDate for date management (EC-098: persisted in SavedStateHandle)
    private val _selectedDate = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_DATE)?.let { LocalDate.parse(it) } ?: LocalDate.now()
    )

    // Current date string for database queries (yyyy-MM-dd format)
    private val _currentDateString = MutableStateFlow(getCurrentDateString())

    companion object {
        private const val KEY_SELECTED_DATE = "selected_date"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    // Main screen state
    private val _uiState = MutableStateFlow(DailyFoodLogUiState())
    val uiState: StateFlow<DailyFoodLogUiState> = _uiState.asStateFlow()

    // Add food state
    private val _addFoodState = MutableStateFlow(AddFoodUiState())
    val addFoodState: StateFlow<AddFoodUiState> = _addFoodState.asStateFlow()

    // Edit food state
    private val _editFoodState = MutableStateFlow(EditFoodUiState())
    val editFoodState: StateFlow<EditFoodUiState> = _editFoodState.asStateFlow()

    // Search query for debounce
    private val _searchQuery = MutableStateFlow("")

    init {
        observeSelectedDate()
        observeEntries()
        observeGoal()
        observeFoodSearch()
    }

    /**
     * FIP-EPIC-005: Observe selected date changes and update UI state accordingly.
     * EC-098: Persists selected date in SavedStateHandle for lifecycle resilience.
     */
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _selectedDate.collect { date ->
                val today = LocalDate.now()
                val isToday = date == today
                val canForward = date < today

                // Persist to SavedStateHandle for process death recovery (EC-098)
                savedStateHandle[KEY_SELECTED_DATE] = date.format(DATE_FORMATTER)

                // Update current date string for database queries
                _currentDateString.value = date.format(DATE_FORMATTER)

                _uiState.update { state ->
                    state.copy(
                        selectedDate = date,
                        isViewingToday = isToday,
                        canNavigateForward = canForward
                    )
                }
            }
        }
    }

    private fun observeEntries() {
        viewModelScope.launch {
            _currentDateString.flatMapLatest { date ->
                foodIntakeRepository.getEntriesForDate(date)
            }.collect { entries ->
                _uiState.update { state ->
                    val totalCals = entries.sumOf { it.entry.totalCalories }
                    val totalProt = entries.sumOf { it.entry.totalProtein }
                    val totalFatVal = entries.sumOf { it.entry.totalFat }
                    val totalCarbsVal = entries.sumOf { it.entry.totalCarbs }

                    val roundedProtein = roundToOneDecimal(totalProt)
                    val roundedFat = roundToOneDecimal(totalFatVal)
                    val roundedCarbs = roundToOneDecimal(totalCarbsVal)

                    // Recalculate progress states with current goal (US-012, AC-054)
                    val goal = state.activeGoal
                    state.copy(
                        entries = entries,
                        totalCalories = totalCals,
                        totalProtein = roundedProtein,
                        totalFat = roundedFat,
                        totalCarbs = roundedCarbs,
                        calorieProgress = computeNutrientProgress(
                            totalCals.toDouble(), goal?.calorieTarget?.toDouble()
                        ),
                        proteinProgress = computeNutrientProgress(
                            roundedProtein, goal?.proteinTarget
                        ),
                        fatProgress = computeNutrientProgress(
                            roundedFat, goal?.fatTarget
                        ),
                        carbsProgress = computeNutrientProgress(
                            roundedCarbs, goal?.carbsTarget
                        ),
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    private fun observeGoal() {
        viewModelScope.launch {
            calorieGoalRepository.getActiveGoal().collect { goal ->
                _uiState.update { state ->
                    // EC-090: Recalculate progress when goal changes mid-day
                    state.copy(
                        activeGoal = goal,
                        calorieProgress = computeNutrientProgress(
                            state.totalCalories.toDouble(), goal?.calorieTarget?.toDouble()
                        ),
                        proteinProgress = computeNutrientProgress(
                            state.totalProtein, goal?.proteinTarget
                        ),
                        fatProgress = computeNutrientProgress(
                            state.totalFat, goal?.fatTarget
                        ),
                        carbsProgress = computeNutrientProgress(
                            state.totalCarbs, goal?.carbsTarget
                        )
                    )
                }
            }
        }
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

    // ============================
    // Date management (EC-001, EC-003, EC-005)
    // FIP-EPIC-005: Historical day viewing (US-013 to US-017)
    // ============================

    /**
     * Refreshes the date check when app resumes (EC-005).
     * If viewing today and midnight has passed, update to new today.
     * If viewing a historical date, maintain that date (EC-098).
     */
    fun refreshDate() {
        val today = LocalDate.now()
        val currentSelected = _selectedDate.value

        // Only auto-update if we were viewing "today" and the day changed
        if (currentSelected >= today.minusDays(1) && currentSelected < today) {
            // The date we were viewing is now yesterday, keep viewing it
            // This handles EC-001: Midnight transition
        }
        // If currentSelected > today (shouldn't happen), reset to today (EC-095)
        if (currentSelected > today) {
            _selectedDate.value = today
        }
    }

    /**
     * FIP-EPIC-005 US-013: Set the selected date for viewing.
     * EC-095: Prevents navigation to future dates.
     * EC-097: Cancels pending loads by updating the date flow.
     *
     * @param date The date to navigate to
     */
    fun setSelectedDate(date: LocalDate) {
        val today = LocalDate.now()
        // EC-095, EC-123: Prevent future date selection
        val safeDate = if (date > today) today else date
        _selectedDate.value = safeDate
    }

    /**
     * FIP-EPIC-005 US-013: Navigate to the previous day.
     * Always allowed (EC-094: supports any past date).
     */
    fun navigateToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    /**
     * FIP-EPIC-005 US-013: Navigate to the next day.
     * EC-095: Blocked when already viewing today.
     */
    fun navigateToNextDay() {
        val current = _selectedDate.value
        val today = LocalDate.now()
        if (current < today) {
            _selectedDate.value = current.plusDays(1)
        }
    }

    /**
     * FIP-EPIC-005 US-013: Quick return to today.
     */
    fun returnToToday() {
        _selectedDate.value = LocalDate.now()
    }

    /**
     * FIP-EPIC-005: Open the date picker dialog.
     */
    fun openDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = true) }
    }

    /**
     * FIP-EPIC-005: Close the date picker dialog.
     */
    fun closeDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = false) }
    }

    /**
     * FIP-EPIC-005: Handle date selection from the date picker.
     * EC-095, EC-123: Validates date is not in the future.
     *
     * @param date The selected date
     */
    fun onDatePickerDateSelected(date: LocalDate) {
        setSelectedDate(date)
        closeDatePicker()
    }

    // ============================
    // Delete entry (US-004)
    // ============================

    fun showDeleteDialog(entry: FoodIntakeWithFood) {
        _uiState.update { it.copy(deleteDialogEntry = entry) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogEntry = null) }
    }

    fun confirmDeleteEntry() {
        val entry = _uiState.value.deleteDialogEntry ?: return
        viewModelScope.launch {
            try {
                foodIntakeRepository.deleteEntry(entry.entry.id)
                _uiState.update {
                    it.copy(
                        deleteDialogEntry = null,
                        snackbarMessage = "Entry deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        deleteDialogEntry = null,
                        error = "Failed to delete entry. Please try again."
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ============================
    // Add food flow (US-002, US-005)
    // ============================

    fun openAddFood() {
        val targetDate = _selectedDate.value
        val today = LocalDate.now()
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
                val today = LocalDate.now()

                // EC-123: Defensive check to prevent future dates
                val safeDate = if (targetDate > today) today else targetDate
                val dateString = safeDate.format(DATE_FORMATTER)

                // For historical entries, use a timestamp at the end of that day
                // For today's entries, use current timestamp
                val timestamp = if (safeDate == today) {
                    System.currentTimeMillis()
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
                _addFoodState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(snackbarMessage = "Entry added") }
            } catch (e: Exception) {
                _addFoodState.update {
                    it.copy(isSaving = false)
                }
                _uiState.update { it.copy(error = "Failed to save entry. Please try again.") }
            }
        }
    }

    // ============================
    // Edit food flow (US-003)
    // ============================

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
                    val today = LocalDate.now()
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
                _editFoodState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(snackbarMessage = "Entry updated") }
            } catch (e: Exception) {
                _editFoodState.update { it.copy(isSaving = false) }
                _uiState.update { it.copy(error = "Failed to update entry. Please try again.") }
            }
        }
    }

    // ============================
    // Validation (EC-011, EC-012, EC-013, EC-014, EC-015, EC-023)
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

    // ============================
    // Progress computation (US-012)
    // ============================

    /**
     * Computes [NutrientProgress] for a given consumed value and optional goal.
     *
     * Implements the color threshold logic from the design spec (C-004):
     * - 0-89%: NORMAL (success green)
     * - 90-100%: APPROACHING (warning orange)
     * - 100%+: EXCEEDED (error red)
     * - No goal: NO_GOAL (no progress bar shown)
     *
     * Handles edge cases:
     * - EC-087: Exactly 100% -> APPROACHING (still within the 90-100% range)
     * - EC-088: 0 calories -> 0% progress
     * - EC-089: Progress beyond 100% is tracked but display capped at 150%
     * - EC-090: Recomputed when goal changes
     * - EC-091: Values rounded to 1 decimal place (via roundToOneDecimal)
     */
    internal fun computeNutrientProgress(consumed: Double, goal: Double?): NutrientProgress {
        if (goal == null || goal <= 0) {
            return NutrientProgress(
                consumed = consumed,
                goal = null,
                progress = null,
                remaining = null,
                progressLevel = ProgressLevel.NO_GOAL
            )
        }

        val progress = (consumed / goal).toFloat()
        val remaining = goal - consumed

        val level = when {
            progress < 0.9f -> ProgressLevel.NORMAL
            progress <= 1.0f -> ProgressLevel.APPROACHING
            else -> ProgressLevel.EXCEEDED
        }

        return NutrientProgress(
            consumed = consumed,
            goal = goal,
            progress = progress,
            remaining = roundToOneDecimal(remaining),
            progressLevel = level
        )
    }

    // ============================
    // Helpers
    // ============================

    /**
     * Gets the current date as a string in yyyy-MM-dd format.
     * Used for backward compatibility with existing queries.
     */
    private fun getCurrentDateString(): String {
        return LocalDate.now().format(DATE_FORMATTER)
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
