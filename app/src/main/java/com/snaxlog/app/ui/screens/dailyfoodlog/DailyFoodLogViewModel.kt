package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * UI state for the Daily Food Log screen (S-001).
 */
data class DailyFoodLogUiState(
    val entries: List<FoodIntakeWithFood> = emptyList(),
    val activeGoal: CalorieGoalEntity? = null,
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val deleteDialogEntry: FoodIntakeWithFood? = null,
    val snackbarMessage: String? = null
)

/**
 * UI state for the Add Food bottom sheet (S-003).
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
    val isLoadingFoods: Boolean = true
)

/**
 * UI state for the Edit Food bottom sheet (S-004).
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
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DailyFoodLogViewModel @Inject constructor(
    private val foodIntakeRepository: FoodIntakeRepository,
    private val foodRepository: FoodRepository,
    private val calorieGoalRepository: CalorieGoalRepository
) : ViewModel() {

    private val _currentDate = MutableStateFlow(getCurrentDate())

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
        observeEntries()
        observeGoal()
        observeFoodSearch()
    }

    private fun observeEntries() {
        viewModelScope.launch {
            _currentDate.flatMapLatest { date ->
                foodIntakeRepository.getEntriesForDate(date)
            }.collect { entries ->
                _uiState.update { state ->
                    val totalCals = entries.sumOf { it.entry.totalCalories }
                    val totalProt = entries.sumOf { it.entry.totalProtein }
                    val totalFatVal = entries.sumOf { it.entry.totalFat }
                    val totalCarbsVal = entries.sumOf { it.entry.totalCarbs }

                    state.copy(
                        entries = entries,
                        totalCalories = totalCals,
                        totalProtein = roundToOneDecimal(totalProt),
                        totalFat = roundToOneDecimal(totalFatVal),
                        totalCarbs = roundToOneDecimal(totalCarbsVal),
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
                _uiState.update { it.copy(activeGoal = goal) }
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
    // ============================

    fun refreshDate() {
        val newDate = getCurrentDate()
        if (newDate != _currentDate.value) {
            _currentDate.value = newDate
        }
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
        _addFoodState.value = AddFoodUiState()
        _searchQuery.value = ""
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
                val entry = FoodIntakeEntryEntity(
                    foodId = food.id,
                    servings = servings,
                    totalCalories = (food.caloriesPerServing * servings).roundToInt(),
                    totalProtein = roundToOneDecimal(food.proteinPerServing * servings),
                    totalFat = roundToOneDecimal(food.fatPerServing * servings),
                    totalCarbs = roundToOneDecimal(food.carbsPerServing * servings),
                    date = getCurrentDate(),
                    timestamp = System.currentTimeMillis()
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
                    _editFoodState.update {
                        it.copy(
                            entry = entryWithFood,
                            servingsInput = servingsStr,
                            previewCalories = entryWithFood.entry.totalCalories,
                            previewProtein = entryWithFood.entry.totalProtein,
                            previewFat = entryWithFood.entry.totalFat,
                            previewCarbs = entryWithFood.entry.totalCarbs,
                            isLoading = false,
                            error = null
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
                val updatedEntry = entryWithFood.entry.copy(
                    servings = servings,
                    totalCalories = (food.caloriesPerServing * servings).roundToInt(),
                    totalProtein = roundToOneDecimal(food.proteinPerServing * servings),
                    totalFat = roundToOneDecimal(food.fatPerServing * servings),
                    totalCarbs = roundToOneDecimal(food.carbsPerServing * servings)
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
    // Helpers
    // ============================

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
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
