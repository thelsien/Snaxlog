package com.snaxlog.app.ui.screens.customfood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.data.repository.FoodRepository
import com.snaxlog.app.data.repository.RecipeIngredientInput
import com.snaxlog.app.ui.components.IngredientItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Create/Edit Recipe form.
 * EPIC-006: US-019, US-021
 */
data class RecipeFormUiState(
    // Mode
    val isEditMode: Boolean = false,
    val editingRecipeId: Long? = null,

    // Form inputs
    val nameInput: String = "",
    val nameError: String? = null,

    val numberOfServingsInput: String = "1",
    val numberOfServingsError: String? = null,

    // Ingredients
    val ingredients: List<IngredientItem> = emptyList(),
    val ingredientsError: String? = null,

    // Ingredient picker state
    val showIngredientPicker: Boolean = false,
    val availableFoods: List<FoodEntity> = emptyList(),
    val ingredientSearchQuery: String = "",
    val isLoadingFoods: Boolean = false,

    // Selected ingredient for quantity input
    val selectedFoodForAdd: FoodEntity? = null,
    val ingredientQuantityInput: String = "1",
    val ingredientUnit: ServingUnit = ServingUnit.SERVING,

    // Calculated nutrition
    val totalCalories: Int = 0,
    val totalProtein: Double = 0.0,
    val totalFat: Double = 0.0,
    val totalCarbs: Double = 0.0,

    // State
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val duplicateNameWarning: Boolean = false,
    val saveSuccess: Boolean = false
)

/**
 * ViewModel for Recipe operations.
 * EPIC-006: User-Created Foods and Recipes
 * US-019: Create Recipe with Multiple Ingredients
 * US-021: Edit Recipes
 * US-022: Delete Recipes
 */
@HiltViewModel
class RecipeViewModel @Inject constructor(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(RecipeFormUiState())
    val formState: StateFlow<RecipeFormUiState> = _formState.asStateFlow()

    // Counter for generating temporary IDs for new ingredients
    private var tempIngredientIdCounter = -1L

    // ============================
    // US-019: Create Recipe
    // ============================

    /**
     * Initialize form for creating a new recipe.
     */
    fun openCreateForm() {
        _formState.value = RecipeFormUiState()
        tempIngredientIdCounter = -1L
    }

    // ============================
    // US-021: Edit Recipe
    // ============================

    /**
     * Initialize form for editing an existing recipe.
     * AC-016-001: Pre-fills form with existing values.
     */
    fun openEditForm(recipeId: Long) {
        _formState.value = RecipeFormUiState(isEditMode = true, isLoading = true)
        tempIngredientIdCounter = -1L

        viewModelScope.launch {
            try {
                val recipeWithIngredients = foodRepository.getRecipeWithIngredients(recipeId)
                if (recipeWithIngredients != null) {
                    val recipe = recipeWithIngredients.recipe

                    // Verify it's actually a recipe
                    if (recipe.foodType != FoodType.RECIPE) {
                        _formState.update {
                            it.copy(isLoading = false, error = "This is not a recipe")
                        }
                        return@launch
                    }

                    // Convert ingredients to IngredientItems (skip missing foods)
                    val ingredientItems = recipeWithIngredients.ingredients
                        .filter { it.food != null }
                        .mapIndexed { index, ingredientWithFood ->
                            val food = ingredientWithFood.food!!
                            // Determine serving display based on food type
                            val servingSizeDisplay = if (food.isUserCreated) {
                                "${food.servingSizeValue} ${food.servingUnit.abbreviation}"
                            } else {
                                food.servingSize
                            }
                            val servingSizeValue = if (food.isUserCreated) {
                                food.servingSizeValue
                            } else {
                                food.servingWeightGrams.takeIf { it > 0 } ?: 1.0
                            }
                            IngredientItem(
                                id = ingredientWithFood.ingredient.id,
                                foodId = food.id,
                                foodName = food.name,
                                quantity = ingredientWithFood.ingredient.quantity,
                                unit = ingredientWithFood.ingredient.unit,
                                caloriesPerServing = food.caloriesPerServing,
                                proteinPerServing = food.proteinPerServing,
                                fatPerServing = food.fatPerServing,
                                carbsPerServing = food.carbsPerServing,
                                servingSizeValue = servingSizeValue,
                                servingSizeDisplay = servingSizeDisplay
                            )
                        }

                    _formState.update {
                        it.copy(
                            isEditMode = true,
                            editingRecipeId = recipe.id,
                            nameInput = recipe.name,
                            numberOfServingsInput = formatDouble(recipe.numberOfServings ?: 1.0),
                            ingredients = ingredientItems,
                            isLoading = false,
                            error = null
                        )
                    }
                    recalculateNutrition()
                } else {
                    _formState.update {
                        it.copy(isLoading = false, error = "Recipe not found")
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isLoading = false, error = "Failed to load recipe")
                }
            }
        }
    }

    // ============================
    // Form input handlers
    // ============================

    /**
     * Update recipe name input.
     * EC-014-006: Limit to 100 characters.
     */
    fun updateName(name: String) {
        val limited = name.take(100)
        val error = validateName(limited)
        _formState.update { it.copy(nameInput = limited, nameError = error) }

        // Check for duplicate name
        if (error == null && limited.isNotBlank()) {
            checkDuplicateName(limited)
        } else {
            _formState.update { it.copy(duplicateNameWarning = false) }
        }
    }

    private fun checkDuplicateName(name: String) {
        viewModelScope.launch {
            val exists = foodRepository.foodNameExists(name)
            val isOwnName = _formState.value.isEditMode &&
                    _formState.value.editingRecipeId != null

            _formState.update {
                it.copy(duplicateNameWarning = exists && !isOwnName)
            }
        }
    }

    /**
     * Update number of servings input.
     * EC-014-004: Must be greater than 0.
     */
    fun updateNumberOfServings(input: String) {
        val filtered = filterNumericInput(input)
        val error = validateNumberOfServings(filtered)
        _formState.update { it.copy(numberOfServingsInput = filtered, numberOfServingsError = error) }
        recalculateNutrition()
    }

    // ============================
    // Ingredient management
    // ============================

    /**
     * Open ingredient picker.
     * AC-014-001: Search foods to add as ingredients.
     */
    fun openIngredientPicker() {
        _formState.update {
            it.copy(
                showIngredientPicker = true,
                ingredientSearchQuery = "",
                isLoadingFoods = true
            )
        }
        loadAvailableFoods("")
    }

    /**
     * Close ingredient picker.
     */
    fun closeIngredientPicker() {
        _formState.update {
            it.copy(
                showIngredientPicker = false,
                selectedFoodForAdd = null,
                ingredientQuantityInput = "1",
                ingredientUnit = ServingUnit.SERVING,
                ingredientSearchQuery = ""
            )
        }
    }

    /**
     * Search for foods to add as ingredients.
     * EC-014-001: Recipes cannot contain other recipes.
     */
    fun searchIngredients(query: String) {
        _formState.update {
            it.copy(ingredientSearchQuery = query, isLoadingFoods = true)
        }
        loadAvailableFoods(query)
    }

    private fun loadAvailableFoods(query: String) {
        viewModelScope.launch {
            try {
                // EC-014-001: Exclude recipes from ingredient search
                foodRepository.searchFoodsForIngredients(query).collect { foods ->
                    // Also exclude the current recipe being edited
                    val filtered = foods.filter { food ->
                        food.id != _formState.value.editingRecipeId
                    }
                    _formState.update {
                        it.copy(availableFoods = filtered, isLoadingFoods = false)
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isLoadingFoods = false, error = "Failed to load foods")
                }
            }
        }
    }

    /**
     * Select a food to add as ingredient.
     */
    fun selectFoodForAdd(food: FoodEntity) {
        _formState.update {
            it.copy(
                selectedFoodForAdd = food,
                ingredientQuantityInput = "1",
                ingredientUnit = food.servingUnit
            )
        }
    }

    /**
     * Clear the selected food (go back to food search).
     */
    fun clearFoodSelection() {
        _formState.update {
            it.copy(
                selectedFoodForAdd = null,
                ingredientQuantityInput = "1",
                ingredientUnit = ServingUnit.SERVING
            )
        }
    }

    /**
     * Update ingredient quantity input.
     */
    fun updateIngredientQuantity(input: String) {
        val filtered = filterNumericInput(input)
        _formState.update { it.copy(ingredientQuantityInput = filtered) }
    }

    /**
     * Update ingredient unit selection.
     */
    fun updateIngredientUnit(unit: ServingUnit) {
        _formState.update { it.copy(ingredientUnit = unit) }
    }

    /**
     * Confirm adding selected food as ingredient.
     * AC-014-002: Add ingredient with quantity and unit.
     */
    fun confirmAddIngredient() {
        val food = _formState.value.selectedFoodForAdd ?: return
        val quantity = _formState.value.ingredientQuantityInput.toDoubleOrNull()

        if (quantity == null || quantity <= 0) {
            _formState.update { it.copy(error = "Please enter a valid quantity") }
            return
        }

        // Determine serving display based on food type
        val servingSizeDisplay = if (food.isUserCreated) {
            "${food.servingSizeValue} ${food.servingUnit.abbreviation}"
        } else {
            food.servingSize
        }
        val servingSizeValue = if (food.isUserCreated) {
            food.servingSizeValue
        } else {
            food.servingWeightGrams.takeIf { it > 0 } ?: 1.0
        }

        val newIngredient = IngredientItem(
            id = tempIngredientIdCounter--,
            foodId = food.id,
            foodName = food.name,
            quantity = quantity,
            unit = _formState.value.ingredientUnit,
            caloriesPerServing = food.caloriesPerServing,
            proteinPerServing = food.proteinPerServing,
            fatPerServing = food.fatPerServing,
            carbsPerServing = food.carbsPerServing,
            servingSizeValue = servingSizeValue,
            servingSizeDisplay = servingSizeDisplay
        )

        _formState.update {
            it.copy(
                ingredients = it.ingredients + newIngredient,
                selectedFoodForAdd = null,
                ingredientQuantityInput = "1",
                ingredientsError = null
            )
        }
        recalculateNutrition()
    }

    /**
     * Remove an ingredient from the recipe.
     */
    fun removeIngredient(ingredientId: Long) {
        _formState.update {
            it.copy(
                ingredients = it.ingredients.filter { ingredient -> ingredient.id != ingredientId }
            )
        }
        recalculateNutrition()
    }

    /**
     * Update ingredient quantity in the list.
     */
    fun updateIngredientInList(ingredientId: Long, newQuantity: Double) {
        if (newQuantity <= 0) return

        _formState.update { state ->
            state.copy(
                ingredients = state.ingredients.map { ingredient ->
                    if (ingredient.id == ingredientId) {
                        ingredient.copy(quantity = newQuantity)
                    } else {
                        ingredient
                    }
                }
            )
        }
        recalculateNutrition()
    }

    /**
     * Update ingredient unit in the list.
     */
    fun updateIngredientUnitInList(ingredientId: Long, newUnit: ServingUnit) {
        _formState.update { state ->
            state.copy(
                ingredients = state.ingredients.map { ingredient ->
                    if (ingredient.id == ingredientId) {
                        ingredient.copy(unit = newUnit)
                    } else {
                        ingredient
                    }
                }
            )
        }
    }

    /**
     * Recalculate total nutrition from all ingredients.
     * AC-014-003: Real-time nutrition updates.
     */
    private fun recalculateNutrition() {
        val ingredients = _formState.value.ingredients

        val totalCalories = ingredients.sumOf { it.totalCalories }
        val totalProtein = ingredients.sumOf { it.totalProtein }
        val totalFat = ingredients.sumOf { it.totalFat }
        val totalCarbs = ingredients.sumOf { it.totalCarbs }

        _formState.update {
            it.copy(
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalFat = totalFat,
                totalCarbs = totalCarbs
            )
        }
    }

    // ============================
    // Save recipe
    // ============================

    /**
     * Save the recipe (create or update).
     * EC-014-002: Validates all inputs before saving.
     * EC-014-003: Minimum 1 ingredient required.
     */
    fun saveRecipe() {
        val state = _formState.value

        // Validate all fields
        val nameError = validateName(state.nameInput)
        val servingsError = validateNumberOfServings(state.numberOfServingsInput)
        val ingredientsError = validateIngredients(state.ingredients)

        if (nameError != null || servingsError != null || ingredientsError != null) {
            _formState.update {
                it.copy(
                    nameError = nameError,
                    numberOfServingsError = servingsError,
                    ingredientsError = ingredientsError
                )
            }
            return
        }

        // EC-014-005: Prevent duplicate submissions
        if (state.isSaving) return
        _formState.update { it.copy(isSaving = true) }

        val numberOfServings = state.numberOfServingsInput.toDoubleOrNull() ?: 1.0

        // Convert IngredientItems to RecipeIngredientInput
        val ingredientInputs = state.ingredients.mapIndexed { index, item ->
            RecipeIngredientInput(
                foodId = item.foodId,
                quantity = item.quantity,
                unit = item.unit,
                sortOrder = index
            )
        }

        viewModelScope.launch {
            try {
                if (state.isEditMode && state.editingRecipeId != null) {
                    // Update existing recipe
                    foodRepository.updateRecipe(
                        recipeId = state.editingRecipeId,
                        name = state.nameInput.trim(),
                        numberOfServings = numberOfServings,
                        ingredients = ingredientInputs
                    )
                    _formState.update {
                        it.copy(isSaving = false, saveSuccess = true)
                    }
                } else {
                    // Create new recipe
                    foodRepository.createRecipe(
                        name = state.nameInput.trim(),
                        numberOfServings = numberOfServings,
                        ingredients = ingredientInputs
                    )
                    _formState.update {
                        it.copy(isSaving = false, saveSuccess = true)
                    }
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(isSaving = false, error = "Failed to save recipe. Please try again.")
                }
            }
        }
    }

    // ============================
    // Utility functions
    // ============================

    fun clearError() {
        _formState.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _formState.update { it.copy(saveSuccess = false) }
    }

    // ============================
    // Validation helpers
    // ============================

    private fun validateName(name: String): String? {
        if (name.isBlank()) return "Recipe name is required"
        return null
    }

    private fun validateNumberOfServings(input: String): String? {
        if (input.isBlank()) return "Number of servings is required"

        val value = input.toDoubleOrNull()
            ?: return "Please enter a valid number"

        if (value <= 0) return "Must be greater than 0"

        return null
    }

    private fun validateIngredients(ingredients: List<IngredientItem>): String? {
        // EC-014-003: Minimum 1 ingredient required
        if (ingredients.isEmpty()) return "Add at least one ingredient"
        return null
    }

    private fun filterNumericInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
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
