package com.snaxlog.app.ui.screens.goalmanagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.ui.components.GoalFormField
import com.snaxlog.app.ui.theme.Spacing

/**
 * S-005: AddEditGoalBottomSheet content
 * Form to create or edit a custom calorie goal.
 */
@Composable
fun AddEditGoalSheetContent(
    viewModel: GoalManagementViewModel,
    onDismiss: () -> Unit
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.bottomSheetPadding)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = if (formState.isEditMode) "Edit Goal" else "Add Custom Goal",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        when {
            formState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.massive),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            formState.error != null && formState.isEditMode && formState.editingGoalId == null -> {
                // Fatal error (goal doesn't exist)
                Text(
                    text = formState.error ?: "Unknown error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Spacing.base))
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }

            else -> {
                // Goal name field
                GoalFormField(
                    value = formState.nameInput,
                    onValueChange = { viewModel.updateGoalName(it) },
                    label = "Goal Name",
                    placeholder = "e.g., My Custom Goal",
                    errorMessage = formState.nameError,
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(Spacing.base))

                // Calorie target field
                GoalFormField(
                    value = formState.calorieInput,
                    onValueChange = { viewModel.updateCalorieTarget(it) },
                    label = "Calorie Target",
                    placeholder = "e.g., 2000",
                    errorMessage = formState.calorieError,
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Optional macro targets section
                Text(
                    text = "Macro Targets (optional)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Protein
                GoalFormField(
                    value = formState.proteinInput,
                    onValueChange = { viewModel.updateProteinTarget(it) },
                    label = "Protein (g)",
                    placeholder = "e.g., 150",
                    errorMessage = formState.proteinError,
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Fat
                GoalFormField(
                    value = formState.fatInput,
                    onValueChange = { viewModel.updateFatTarget(it) },
                    label = "Fat (g)",
                    placeholder = "e.g., 67",
                    errorMessage = formState.fatError,
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Carbs
                GoalFormField(
                    value = formState.carbsInput,
                    onValueChange = { viewModel.updateCarbsTarget(it) },
                    label = "Carbs (g)",
                    placeholder = "e.g., 200",
                    errorMessage = formState.carbsError,
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !formState.isSaving
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(Spacing.sm))

                    Button(
                        onClick = { viewModel.saveGoal() },
                        enabled = !formState.isSaving && formState.nameInput.isNotBlank() && formState.calorieInput.isNotBlank()
                    ) {
                        if (formState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(Spacing.lg)
                                    .width(Spacing.lg),
                                strokeWidth = Spacing.xxs
                            )
                        } else {
                            Text(if (formState.isEditMode) "Update" else "Save")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))
            }
        }
    }
}
