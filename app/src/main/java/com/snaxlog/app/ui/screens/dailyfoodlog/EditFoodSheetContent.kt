package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.ui.components.MealCategorySelector
import com.snaxlog.app.ui.components.NutritionPreview
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import com.snaxlog.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * S-004: EditFoodBottomSheet content
 * Edit existing food entry's serving size.
 *
 * FIP-EPIC-005 US-015: Supports editing entries on historical dates with date display.
 */
@Composable
fun EditFoodSheetContent(
    viewModel: DailyFoodLogViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.editFoodState.collectAsStateWithLifecycle()
    val customColors = SnaxlogThemeExtras.customColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.bottomSheetPadding)
    ) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(Spacing.base))
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }

            state.entry != null -> {
                val entryWithFood = state.entry!!
                val food = entryWithFood.food

                // FIP-EPIC-005 US-015: Show date context for historical entries
                val entryDate = state.entryDate
                if (state.isEditingHistorical && entryDate != null) {
                    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
                    val formattedDate = entryDate.format(dateFormatter)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(customColors.historicalDateBackground)
                            .padding(horizontal = Spacing.base, vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Entry from: $formattedDate",
                            style = MaterialTheme.typography.bodySmall,
                            color = customColors.historicalDate
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }

                Text(
                    text = "Edit Entry",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.base))

                // Food name (read-only display)
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Serving: ${food.servingSize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.base))

                // Serving size input
                OutlinedTextField(
                    value = state.servingsInput,
                    onValueChange = { viewModel.updateEditFoodServings(it) },
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

                // FIP-005: MealCategorySelector - shows current category, no auto-selection
                MealCategorySelector(
                    selectedCategory = state.selectedCategory,
                    onCategoryChange = { viewModel.updateEditFoodCategory(it) },
                    autoSelectedCategory = null, // No auto-selection animation in edit mode
                    showLabel = true
                )

                Spacer(modifier = Modifier.height(Spacing.base))

                // Nutrition preview with real-time updates (AC-013)
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
                        onClick = {
                            viewModel.saveEditFood()
                        },
                        enabled = state.servingsError == null && !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Entry")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))
            }
        }
    }
}
