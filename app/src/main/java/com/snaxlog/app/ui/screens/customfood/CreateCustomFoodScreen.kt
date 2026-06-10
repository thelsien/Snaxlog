package com.snaxlog.app.ui.screens.customfood

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.ui.components.CalorieCalculator
import com.snaxlog.app.ui.components.MacroInputField
import com.snaxlog.app.ui.components.MacroType
import com.snaxlog.app.ui.components.ServingUnitDropdown
import com.snaxlog.app.ui.theme.Spacing

/**
 * S-006: CreateCustomFoodScreen
 * EPIC-006: User-Created Foods and Recipes
 * US-018: Create Simple Custom Food
 *
 * Full-screen form for creating or editing a simple custom food.
 * Includes name, serving size with unit, macro inputs, and auto-calculated calories.
 *
 * @param onNavigateBack Callback to navigate back.
 * @param editFoodId Optional food ID for edit mode (null for create mode).
 * @param viewModel ViewModel for form state management.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomFoodScreen(
    onNavigateBack: () -> Unit,
    editFoodId: Long? = null,
    viewModel: CustomFoodViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Initialize form based on mode
    LaunchedEffect(editFoodId) {
        if (editFoodId != null) {
            viewModel.openEditForm(editFoodId)
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

    // Handle errors
    LaunchedEffect(formState.error) {
        formState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (formState.isEditMode) "Edit Custom Food" else "Create Custom Food"
                    )
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (formState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
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
                // Food Name Input
                // AC-013-001: Food name is required
                OutlinedTextField(
                    value = formState.nameInput,
                    onValueChange = { viewModel.updateName(it) },
                    label = { Text("Food Name") },
                    placeholder = { Text("e.g., Homemade Granola") },
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Food name input"
                            formState.nameError?.let { error(it) }
                        }
                )

                // EC-013-007: Duplicate name warning
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
                            text = "A food with this name already exists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))

                // Serving Size Section
                Text(
                    text = "Serving Size",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // AC-013-003: Serving size with unit selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    // Serving size value
                    OutlinedTextField(
                        value = formState.servingSizeInput,
                        onValueChange = { viewModel.updateServingSize(it) },
                        label = { Text("Amount") },
                        placeholder = { Text("100") },
                        isError = formState.servingSizeError != null,
                        supportingText = formState.servingSizeError?.let { error ->
                            { Text(error, color = MaterialTheme.colorScheme.error) }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .semantics {
                                contentDescription = "Serving size amount"
                                formState.servingSizeError?.let { error(it) }
                            }
                    )

                    // Serving unit dropdown
                    ServingUnitDropdown(
                        selectedUnit = formState.servingUnit,
                        onUnitChange = { viewModel.updateServingUnit(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                // Macro Nutrients Section
                Text(
                    text = "Nutritional Information (per serving)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // AC-013-001: Protein, Fat, Carbs inputs
                MacroInputField(
                    value = formState.proteinInput,
                    onValueChange = { viewModel.updateProtein(it) },
                    macroType = MacroType.PROTEIN,
                    errorMessage = formState.proteinError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                MacroInputField(
                    value = formState.fatInput,
                    onValueChange = { viewModel.updateFat(it) },
                    macroType = MacroType.FAT,
                    errorMessage = formState.fatError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                MacroInputField(
                    value = formState.carbsInput,
                    onValueChange = { viewModel.updateCarbs(it) },
                    macroType = MacroType.CARBS,
                    errorMessage = formState.carbsError,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                // AC-013-002: Auto-calculated calories
                CalorieCalculator(
                    protein = formState.proteinInput.toDoubleOrNull() ?: 0.0,
                    fat = formState.fatInput.toDoubleOrNull() ?: 0.0,
                    carbs = formState.carbsInput.toDoubleOrNull() ?: 0.0,
                    showFormula = true
                )

                Spacer(modifier = Modifier.height(Spacing.xxl))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Button(
                        onClick = { viewModel.saveCustomFood() },
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
                            Text(if (formState.isEditMode) "Save Changes" else "Create Food")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.base))
            }
        }
    }
}
