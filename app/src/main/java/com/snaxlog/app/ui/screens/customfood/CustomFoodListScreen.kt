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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.FoodTypeBadge
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat

/**
 * S-007: CustomFoodListScreen
 * EPIC-006: User-Created Foods and Recipes
 * US-020: View and Use Custom Foods in Food Search
 *
 * Displays a list of all user-created foods (both simple and recipes).
 * Provides options to add new, edit, or delete custom foods.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param onNavigateToCreateFood Callback to navigate to create food screen.
 * @param onNavigateToEditFood Callback to navigate to edit food screen with food ID.
 * @param onNavigateToCreateRecipe Callback to navigate to create recipe screen.
 * @param onNavigateToEditRecipe Callback to navigate to edit recipe screen with food ID.
 * @param viewModel ViewModel for list state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateFood: () -> Unit,
    onNavigateToEditFood: (Long) -> Unit,
    onNavigateToCreateRecipe: () -> Unit,
    onNavigateToEditRecipe: (Long) -> Unit,
    viewModel: CustomFoodViewModel = hiltViewModel()
) {
    val listState by viewModel.listState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val numberFormat = NumberFormat.getNumberInstance()

    // Handle snackbar messages
    LaunchedEffect(listState.snackbarMessage) {
        listState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    // Handle errors
    LaunchedEffect(listState.error) {
        listState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Foods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateFood,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom food"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                listState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                listState.foods.isEmpty() -> {
                    EmptyStateView(
                        title = "No custom foods yet",
                        message = "Tap the + button to create your first custom food or recipe"
                    )
                }

                else -> {
                    // Separate foods by type
                    val simpleFoods = listState.foods.filter { it.foodType == FoodType.SIMPLE }
                    val recipes = listState.foods.filter { it.foodType == FoodType.RECIPE }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Simple Custom Foods Section
                        if (simpleFoods.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Custom Foods",
                                    count = simpleFoods.size
                                )
                            }
                            items(
                                items = simpleFoods,
                                key = { "food_${it.id}" }
                            ) { food ->
                                CustomFoodListItem(
                                    food = food,
                                    numberFormat = numberFormat,
                                    onEdit = { onNavigateToEditFood(food.id) },
                                    onDelete = { viewModel.showDeleteDialog(food) }
                                )
                                HorizontalDivider()
                            }
                        }

                        // Recipes Section
                        if (recipes.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "Recipes",
                                    count = recipes.size
                                )
                            }
                            items(
                                items = recipes,
                                key = { "recipe_${it.id}" }
                            ) { recipe ->
                                CustomFoodListItem(
                                    food = recipe,
                                    numberFormat = numberFormat,
                                    onEdit = { onNavigateToEditRecipe(recipe.id) },
                                    onDelete = { viewModel.showDeleteDialog(recipe) }
                                )
                                HorizontalDivider()
                            }
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(Spacing.fabClearance))
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    // US-022: Delete Custom Foods and Recipes
    listState.deleteDialogFood?.let { food ->
        DeleteCustomFoodDialog(
            food = food,
            warningMessage = listState.deleteWarningMessage,
            onConfirm = { viewModel.confirmDeleteFood() },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }
}

/**
 * Section header for grouping foods.
 */
@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.screenPadding,
                vertical = Spacing.sm
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count ${if (count == 1) "item" else "items"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * List item for a custom food or recipe.
 * AC-015-001: Shows badge indicating Custom or Recipe type.
 */
@Composable
private fun CustomFoodListItem(
    food: FoodEntity,
    numberFormat: NumberFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(
                horizontal = Spacing.screenPadding,
                vertical = Spacing.listItemPaddingVertical
            )
            .semantics {
                contentDescription = "${food.name}, ${food.caloriesPerServing} calories per serving. Tap to edit."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Food info
        Column(
            modifier = Modifier.weight(1f)
        ) {
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
                Spacer(modifier = Modifier.width(Spacing.sm))
                FoodTypeBadge(
                    foodType = food.foodType,
                    showLabel = true
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxs))

            // Serving info
            Text(
                text = "${food.servingSizeValue.formatForDisplay()} ${food.servingUnit.abbreviation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Nutrition summary
            Text(
                text = "P: ${food.proteinPerServing.formatForDisplay()}g | F: ${food.fatPerServing.formatForDisplay()}g | C: ${food.carbsPerServing.formatForDisplay()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Calories
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = numberFormat.format(food.caloriesPerServing),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = " cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action buttons
        Spacer(modifier = Modifier.width(Spacing.sm))

        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit ${food.name}",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete ${food.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Delete confirmation dialog.
 * AC-017-001: Shows confirmation before permanent deletion.
 * EC-016-002, EC-016-003: Shows warnings about usage in logs/recipes.
 */
@Composable
private fun DeleteCustomFoodDialog(
    food: FoodEntity,
    warningMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Delete ${if (food.foodType == FoodType.RECIPE) "Recipe" else "Food"}?")
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete \"${food.name}\"?"
                )
                if (warningMessage != null) {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = warningMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(Spacing.sm)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
