package com.snaxlog.app.ui.screens.customfood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.R
import com.snaxlog.app.ui.common.asString
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.FoodTypeBadge
import com.snaxlog.app.ui.components.IngredientListItem
import com.snaxlog.app.ui.components.RecipeNutritionSummary
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat

/**
 * S-008: CreateRecipeScreen
 * EPIC-006: User-Created Foods and Recipes
 * US-019: Create Recipe with Multiple Ingredients
 *
 * Full-screen form for creating or editing a recipe.
 * Includes name, servings, ingredient list, and calculated nutrition.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param editRecipeId Optional recipe ID for edit mode (null for create mode).
 * @param viewModel ViewModel for form state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRecipeScreen(
    onNavigateBack: () -> Unit,
    editRecipeId: Long? = null,
    viewModel: RecipeViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Initialize form based on mode
    LaunchedEffect(editRecipeId) {
        if (editRecipeId != null) {
            viewModel.openEditForm(editRecipeId)
        } else {
            viewModel.openCreateForm()
        }
    }

    // Handle save success
    LaunchedEffect(formState.saveSuccess) {
        if (formState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    val context = LocalContext.current

    // Handle errors
    LaunchedEffect(formState.error) {
        formState.error?.let { error ->
            snackbarHostState.showSnackbar(error.asString(context))
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (formState.isEditMode) {
                            stringResource(R.string.create_recipe_title_edit)
                        } else {
                            stringResource(R.string.create_recipe_title_create)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (formState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.screenPadding)
            ) {
                // Recipe Name Input
                val recipeNameDescription = stringResource(R.string.create_recipe_name_input_description)
                val nameErrorText = formState.nameError?.asString()
                OutlinedTextField(
                    value = formState.nameInput,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text(stringResource(R.string.create_recipe_name_label)) },
                    placeholder = { Text(stringResource(R.string.create_recipe_name_placeholder)) },
                    isError = nameErrorText != null,
                    supportingText = nameErrorText?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = recipeNameDescription
                            nameErrorText?.let { error(it) }
                        }
                )

                // Duplicate name warning
                if (formState.duplicateNameWarning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = Spacing.xs)
                        )
                        Text(
                            text = stringResource(R.string.create_recipe_duplicate_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))

                // Number of Servings Input
                val servingsDescription = stringResource(R.string.create_recipe_servings_description)
                val servingsErrorText = formState.numberOfServingsError?.asString()
                OutlinedTextField(
                    value = formState.numberOfServingsInput,
                    onValueChange = { viewModel.updateNumberOfServings(it) },
                    label = { Text(stringResource(R.string.create_recipe_servings_label)) },
                    placeholder = { Text(stringResource(R.string.create_recipe_servings_placeholder)) },
                    isError = servingsErrorText != null,
                    supportingText = servingsErrorText?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = servingsDescription
                            servingsErrorText?.let { error(it) }
                        }
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Ingredients Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.create_recipe_ingredients),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { viewModel.openIngredientPicker() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Spacing.xs)
                        )
                        Text(stringResource(R.string.create_recipe_add_ingredient))
                    }
                }

                // Ingredients error
                formState.ingredientsError?.let { ingredientsError ->
                    Text(
                        text = ingredientsError.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Ingredients List
                if (formState.ingredients.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.create_recipe_no_ingredients),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(Spacing.base)
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            formState.ingredients.forEachIndexed { index, ingredient ->
                                IngredientListItem(
                                    ingredient = ingredient,
                                    index = index + 1,
                                    onQuantityChange = { newQty ->
                                        viewModel.updateIngredientInList(ingredient.id, newQty)
                                    },
                                    onRemove = { viewModel.removeIngredient(ingredient.id) },
                                    showDragHandle = false
                                )
                                if (index < formState.ingredients.size - 1) {
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Nutrition Summary
                // AC-014-003: Real-time nutrition calculation
                val numberOfServings = formState.numberOfServingsInput.toDoubleOrNull() ?: 1.0
                RecipeNutritionSummary(
                    totalCalories = formState.totalCalories,
                    totalProtein = formState.totalProtein,
                    totalFat = formState.totalFat,
                    totalCarbs = formState.totalCarbs,
                    numberOfServings = numberOfServings,
                    ingredientCount = formState.ingredients.size
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.create_recipe_cancel))
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Button(
                        onClick = { viewModel.saveRecipe() },
                        enabled = !formState.isSaving
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (formState.isEditMode) {
                                    stringResource(R.string.create_recipe_save_changes)
                                } else {
                                    stringResource(R.string.create_recipe_create)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))
            }
        }
    }

    // Ingredient Picker Bottom Sheet
    if (formState.showIngredientPicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeIngredientPicker() },
            sheetState = sheetState
        ) {
            IngredientPickerContent(
                formState = formState,
                viewModel = viewModel
            )
        }
    }
}

/**
 * Content for the ingredient picker bottom sheet.
 * AC-014-001: Search and select foods as ingredients.
 * EC-014-001: Recipes cannot be selected as ingredients.
 */
@Composable
private fun IngredientPickerContent(
    formState: RecipeFormUiState,
    viewModel: RecipeViewModel
) {
    val numberFormat = NumberFormat.getNumberInstance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.bottomSheetPadding)
    ) {
        if (formState.selectedFoodForAdd == null) {
            // Step 1: Search and select food
            Text(
                text = stringResource(R.string.create_recipe_picker_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.base))

            // Search field
            val searchDescription = stringResource(R.string.create_recipe_picker_search_description)
            OutlinedTextField(
                value = formState.ingredientSearchQuery,
                onValueChange = { viewModel.searchIngredients(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = searchDescription },
                placeholder = { Text(stringResource(R.string.create_recipe_picker_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (formState.ingredientSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchIngredients("") }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.create_recipe_picker_clear_search))
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Food list
            if (formState.isLoadingFoods) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(Spacing.xl)
                )
            } else if (formState.availableFoods.isEmpty()) {
                EmptyStateView(
                    title = stringResource(R.string.create_recipe_picker_no_foods_title),
                    message = stringResource(R.string.create_recipe_picker_no_foods_message)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = formState.availableFoods,
                        key = { "food_${it.id}" }
                    ) { food ->
                        FoodPickerItem(
                            food = food,
                            numberFormat = numberFormat,
                            onClick = { viewModel.selectFoodForAdd(food) }
                        )
                        HorizontalDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.base))

            TextButton(
                onClick = { viewModel.closeIngredientPicker() },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.create_recipe_picker_cancel))
            }
        } else {
            // Step 2: Specify quantity for selected food
            val food = formState.selectedFoodForAdd

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearFoodSelection() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.create_recipe_picker_back))
                }
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(Spacing.base))

            // Food info - use appropriate serving display based on food type
            val servingDisplay = if (food.isUserCreated) {
                stringResource(
                    R.string.create_recipe_picker_serving_amount_unit,
                    food.servingSizeValue.formatForDisplay(),
                    food.servingUnit.abbreviation
                )
            } else {
                food.servingSize
            }

            Text(
                text = stringResource(R.string.create_recipe_picker_nutrition_per, servingDisplay),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.create_recipe_picker_nutrition_line,
                    food.caloriesPerServing,
                    food.proteinPerServing.formatForDisplay(),
                    food.fatPerServing.formatForDisplay(),
                    food.carbsPerServing.formatForDisplay()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.base))

            // Quantity input - uses food's serving unit
            OutlinedTextField(
                value = formState.ingredientQuantityInput,
                onValueChange = { viewModel.updateIngredientQuantity(it) },
                label = { Text(stringResource(R.string.create_recipe_picker_servings_label)) },
                supportingText = {
                    Text(stringResource(R.string.create_recipe_picker_serving_equals, servingDisplay))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.closeIngredientPicker() }) {
                    Text(stringResource(R.string.create_recipe_picker_cancel))
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(
                    onClick = {
                        viewModel.confirmAddIngredient()
                        viewModel.closeIngredientPicker()
                    }
                ) {
                    Text(stringResource(R.string.create_recipe_picker_add))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.base))
        }
    }
}

/**
 * Food item for the ingredient picker.
 */
@Composable
private fun FoodPickerItem(
    food: FoodEntity,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    // For pre-loaded foods, use servingSize string; for custom foods, use servingSizeValue + unit
    val servingDisplay = if (food.isUserCreated) {
        stringResource(
            R.string.create_recipe_picker_serving_amount_unit,
            food.servingSizeValue.formatForDisplay(),
            food.servingUnit.abbreviation
        )
    } else {
        food.servingSize
    }
    val pickerItemDescription = stringResource(
        R.string.create_recipe_picker_food_description,
        food.name,
        food.caloriesPerServing
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Spacing.listItemPaddingHorizontal,
                vertical = Spacing.listItemPaddingVertical
            )
            .semantics {
                contentDescription = pickerItemDescription
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (food.isUserCreated) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    FoodTypeBadge(foodType = food.foodType, showLabel = false)
                }
            }
            Text(
                text = servingDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = numberFormat.format(food.caloriesPerServing),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = stringResource(R.string.create_recipe_picker_cal_suffix),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extension to format double values for display.
 */
private fun Double.formatForDisplay(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format("%.1f", this)
    }
}
