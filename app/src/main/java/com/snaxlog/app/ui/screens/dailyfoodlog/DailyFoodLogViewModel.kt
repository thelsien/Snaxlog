package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.R
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
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
    val error: UiText? = null,
    val deleteDialogEntry: FoodIntakeWithFood? = null,
    val snackbarMessage: UiText? = null,
    // FIP-EPIC-005: Historical Day Viewing fields
    val selectedDate: LocalDate = LocalDate.now(),
    val isViewingToday: Boolean = true,
    val isDatePickerOpen: Boolean = false,
    val canNavigateForward: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyFoodLogViewModel @Inject constructor(
    private val foodIntakeRepository: FoodIntakeRepository,
    private val calorieGoalRepository: CalorieGoalRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock
) : ViewModel() {

    // FIP-EPIC-005: Use LocalDate for date management (EC-098: persisted in SavedStateHandle)
    private val _selectedDate = MutableStateFlow(
        savedStateHandle.get<String>(KEY_SELECTED_DATE)?.let { LocalDate.parse(it) } ?: LocalDate.now(clock)
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

    init {
        observeSelectedDate()
        observeEntries()
        observeGoal()
    }

    /**
     * FIP-EPIC-005: Observe selected date changes and update UI state accordingly.
     * EC-098: Persists selected date in SavedStateHandle for lifecycle resilience.
     */
    private fun observeSelectedDate() {
        viewModelScope.launch {
            _selectedDate.collect { date ->
                val today = LocalDate.now(clock)
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
        val today = LocalDate.now(clock)
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
        val today = LocalDate.now(clock)
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
        val today = LocalDate.now(clock)
        if (current < today) {
            _selectedDate.value = current.plusDays(1)
        }
    }

    /**
     * FIP-EPIC-005 US-013: Quick return to today.
     */
    fun returnToToday() {
        _selectedDate.value = LocalDate.now(clock)
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
                        snackbarMessage = UiText.StringResource(R.string.daily_log_msg_entry_deleted)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        deleteDialogEntry = null,
                        error = UiText.StringResource(R.string.daily_log_msg_delete_failed)
                    )
                }
            }
        }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Displays a snackbar message on the daily log.
     *
     * The add/edit-food flows now live in [AddFoodViewModel] / [EditFoodViewModel]; the screen
     * forwards their success results here (as [UiText] resources, e.g. the "Entry added" /
     * "Entry updated" strings) so the snackbar continues to surface on the main screen.
     */
    fun showSnackbar(message: UiText) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    /**
     * Surfaces an error message on the daily log (e.g. a failed add/edit save).
     */
    fun showError(message: UiText) {
        _uiState.update { it.copy(error = message) }
    }

    /**
     * Exposes the currently selected date so the screen can pass it explicitly to
     * [AddFoodViewModel.openAddFood] (entries are logged to the selected day, US-017).
     */
    fun currentSelectedDate(): LocalDate = _selectedDate.value

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
        return LocalDate.now(clock).format(DATE_FORMATTER)
    }

    private fun roundToOneDecimal(value: Double): Double {
        return (value * 10).roundToInt() / 10.0
    }
}
