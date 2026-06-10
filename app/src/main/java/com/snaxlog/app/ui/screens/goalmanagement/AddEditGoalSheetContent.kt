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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.R
import com.snaxlog.app.ui.common.asString
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
            text = if (formState.isEditMode) {
                stringResource(R.string.goal_form_title_edit)
            } else {
                stringResource(R.string.goal_form_title_add)
            },
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
                    text = formState.error?.asString() ?: stringResource(R.string.common_unknown_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(Spacing.base))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.goal_form_close))
                }
            }

            else -> {
                // Goal name field
                GoalFormField(
                    value = formState.nameInput,
                    onValueChange = { viewModel.updateGoalName(it) },
                    label = stringResource(R.string.goal_form_name_label),
                    placeholder = stringResource(R.string.goal_form_name_placeholder),
                    errorMessage = formState.nameError?.asString(),
                    keyboardType = KeyboardType.Text
                )

                Spacer(modifier = Modifier.height(Spacing.base))

                // Calorie target field
                GoalFormField(
                    value = formState.calorieInput,
                    onValueChange = { viewModel.updateCalorieTarget(it) },
                    label = stringResource(R.string.goal_form_calorie_label),
                    placeholder = stringResource(R.string.goal_form_calorie_placeholder),
                    errorMessage = formState.calorieError?.asString(),
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Optional macro targets section
                Text(
                    text = stringResource(R.string.goal_form_macro_section),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Protein
                GoalFormField(
                    value = formState.proteinInput,
                    onValueChange = { viewModel.updateProteinTarget(it) },
                    label = stringResource(R.string.goal_form_protein_label),
                    placeholder = stringResource(R.string.goal_form_protein_placeholder),
                    errorMessage = formState.proteinError?.asString(),
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Fat
                GoalFormField(
                    value = formState.fatInput,
                    onValueChange = { viewModel.updateFatTarget(it) },
                    label = stringResource(R.string.goal_form_fat_label),
                    placeholder = stringResource(R.string.goal_form_fat_placeholder),
                    errorMessage = formState.fatError?.asString(),
                    keyboardType = KeyboardType.Decimal
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Carbs
                GoalFormField(
                    value = formState.carbsInput,
                    onValueChange = { viewModel.updateCarbsTarget(it) },
                    label = stringResource(R.string.goal_form_carbs_label),
                    placeholder = stringResource(R.string.goal_form_carbs_placeholder),
                    errorMessage = formState.carbsError?.asString(),
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
                        Text(stringResource(R.string.goal_form_cancel))
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
                            Text(
                                if (formState.isEditMode) {
                                    stringResource(R.string.goal_form_update)
                                } else {
                                    stringResource(R.string.goal_form_save)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))
            }
        }
    }
}
