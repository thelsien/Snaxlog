package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.MealCategorySelector
import com.snaxlog.app.ui.components.NutritionPreview
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * S-003: AddFoodBottomSheet content
 * Search and select food from database, specify serving size.
 *
 * EPIC-003 / US-011 / AC-048: Foods are displayed organized by category
 * with sticky category headers when browsing (no active search query).
 *
 * FIP-EPIC-005 US-017: Supports adding entries to historical dates.
 */
@Composable
fun AddFoodSheetContent(
    viewModel: DailyFoodLogViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.addFoodState.collectAsStateWithLifecycle()
    val numberFormat = NumberFormat.getNumberInstance()
    val customColors = SnaxlogThemeExtras.customColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.bottomSheetPadding)
    ) {
        if (state.selectedFood == null) {
            // STEP 1: Search and select food
            // FIP-EPIC-005 US-017: Show date context when adding to historical date
            if (state.isAddingToHistorical) {
                val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
                val formattedDate = state.targetDate.format(dateFormatter)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(customColors.historicalDateBackground)
                        .padding(horizontal = Spacing.base, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Adding entry to: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = customColors.historicalDate
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            Text(
                text = "Add Food Entry",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.base))

            // C-007: Search field
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search foods" },
                placeholder = { Text("Search foods...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Food list
            if (state.isLoadingFoods) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(Spacing.xl)
                )
            } else if (state.foods.isEmpty()) {
                EmptyStateView(
                    title = "No foods found",
                    message = "Try different search terms or browse all foods"
                )
            } else {
                // AC-048: Group foods by category for organized browsing.
                // When searching, show flat list (search results may span categories).
                // When browsing (empty query), show category headers.
                val isSearching = state.searchQuery.isNotBlank()
                val groupedFoods = remember(state.foods, isSearching) {
                    if (isSearching) {
                        // Flat list for search results
                        state.foods.map { FoodListItem.FoodItem(it) }
                    } else {
                        // Grouped by category with headers (EC-085 fallback: if
                        // all categories are empty string, shows flat list)
                        val grouped = state.foods.groupBy { it.category }
                        buildList<FoodListItem> {
                            for ((category, foods) in grouped) {
                                if (category.isNotBlank()) {
                                    add(FoodListItem.CategoryHeader(category))
                                }
                                addAll(foods.map { FoodListItem.FoodItem(it) })
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(
                        items = groupedFoods,
                        key = { item ->
                            when (item) {
                                is FoodListItem.CategoryHeader -> "header_${item.category}"
                                is FoodListItem.FoodItem -> "food_${item.food.id}"
                            }
                        }
                    ) { item ->
                        when (item) {
                            is FoodListItem.CategoryHeader -> {
                                CategoryHeaderRow(category = item.category)
                            }
                            is FoodListItem.FoodItem -> {
                                FoodItemRow(
                                    food = item.food,
                                    numberFormat = numberFormat,
                                    onClick = { viewModel.selectFood(item.food) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.base))

            // Cancel button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Cancel")
            }

        } else {
            // STEP 2: Specify servings for selected food
            val food = state.selectedFood!!

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.clearFoodSelection() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to food list")
                }
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(Spacing.base))

            // AC-049: Show serving size
            Text(
                text = "Serving: ${food.servingSize}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // AC-050: Show complete nutritional info (category displayed for context)
            Text(
                text = "Category: ${food.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.base))

            // C-009: ServingSizeInput
            OutlinedTextField(
                value = state.servingsInput,
                onValueChange = { viewModel.updateAddFoodServings(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Number of servings" },
                label = { Text("Number of servings") },
                isError = state.servingsError != null,
                supportingText = state.servingsError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.base))

            // FIP-005: MealCategorySelector - auto-selects based on current time
            // FIP-EPIC-005 US-017: No auto-selection for historical dates
            MealCategorySelector(
                selectedCategory = state.selectedCategory,
                onCategoryChange = { viewModel.updateAddFoodCategory(it) },
                autoSelectedCategory = if (state.isAddingToHistorical) null else state.autoSelectedCategory,
                showLabel = true
            )

            // FIP-EPIC-005 US-017: Show hint for historical entries
            if (state.isAddingToHistorical) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = "Tip: Select the meal category manually for historical entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.base))

            // C-016: NutritionPreview - AC-050: complete nutritional info
            NutritionPreview(
                totalCalories = state.previewCalories,
                totalProtein = state.previewProtein,
                totalFat = state.previewFat,
                totalCarbs = state.previewCarbs
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Button(
                    onClick = { viewModel.saveAddFood() },
                    enabled = state.servingsError == null && !state.isSaving
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Add Entry")
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.base))
        }
    }
}

/**
 * Sealed class representing items in the food list.
 * Either a category header or a food item.
 */
private sealed class FoodListItem {
    data class CategoryHeader(val category: String) : FoodListItem()
    data class FoodItem(val food: FoodEntity) : FoodListItem()
}

/**
 * Category header row displayed above each food group.
 * AC-048: Foods organized by category.
 */
@Composable
private fun CategoryHeaderRow(category: String) {
    Text(
        text = category,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(
                horizontal = Spacing.listItemPaddingHorizontal,
                vertical = Spacing.sm
            )
            .semantics { heading() }
    )
}

/**
 * Individual food item row.
 * AC-049: Shows food name, standard serving size, and calories per serving.
 */
@Composable
private fun FoodItemRow(
    food: FoodEntity,
    numberFormat: NumberFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = Spacing.listItemPaddingHorizontal,
                vertical = Spacing.listItemPaddingVertical
            )
            .semantics {
                contentDescription = "${food.name}, ${food.servingSize}, ${food.caloriesPerServing} calories per serving. Tap to select."
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = food.servingSize,
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
                text = " cal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
