package com.snaxlog.app.ui.screens.goalmanagement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.snaxlog.app.ui.components.DeleteConfirmationDialog
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.GoalCard
import com.snaxlog.app.ui.theme.Spacing

/**
 * S-002: GoalManagementScreen
 * View and manage calorie goals.
 * Displays predefined and custom goals with the ability to select, add, edit, and delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalManagementScreen(
    viewModel: GoalManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Bottom sheet state
    var showGoalFormSheet by remember { mutableStateOf(false) }
    val goalFormSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()

            if (message == "Goal saved") {
                showGoalFormSheet = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Goals",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                FloatingActionButton(
                    onClick = {
                        viewModel.openAddGoalForm()
                        showGoalFormSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "Add custom goal"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                if (uiState.goals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        EmptyStateView(
                            title = "No goals available",
                            message = "Add a custom goal to get started"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(
                            top = Spacing.sm,
                            bottom = Spacing.fabClearance
                        )
                    ) {
                        // AC-028: Prompt to select a goal if none active
                        if (uiState.activeGoalId == null) {
                            item(key = "prompt") {
                                Text(
                                    text = "Select a goal to start tracking your progress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(
                                        horizontal = Spacing.screenPadding,
                                        vertical = Spacing.sm
                                    )
                                )
                            }
                        }

                        items(
                            items = uiState.goals,
                            key = { it.id }
                        ) { goal ->
                            GoalCard(
                                goalName = goal.name,
                                calorieTarget = goal.calorieTarget,
                                isActive = goal.id == uiState.activeGoalId,
                                isPredefined = goal.isPredefined,
                                proteinTarget = goal.proteinTarget,
                                fatTarget = goal.fatTarget,
                                carbsTarget = goal.carbsTarget,
                                onClick = { viewModel.selectGoal(goal.id) },
                                onEdit = if (!goal.isPredefined) {
                                    {
                                        viewModel.openEditGoalForm(goal.id)
                                        showGoalFormSheet = true
                                    }
                                } else null,
                                onDelete = if (!goal.isPredefined) {
                                    { viewModel.showDeleteDialog(goal) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        uiState.deleteDialogGoal?.let { goal ->
            DeleteConfirmationDialog(
                title = "Delete goal?",
                message = if (goal.id == uiState.activeGoalId) {
                    "This is your active goal. Deleting it will remove your daily tracking target."
                } else {
                    "\"${goal.name}\" will be permanently removed."
                },
                onConfirm = { viewModel.confirmDeleteGoal() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }

        // Add/Edit goal bottom sheet
        if (showGoalFormSheet) {
            ModalBottomSheet(
                onDismissRequest = { showGoalFormSheet = false },
                sheetState = goalFormSheetState
            ) {
                AddEditGoalSheetContent(
                    viewModel = viewModel,
                    onDismiss = { showGoalFormSheet = false }
                )
            }
        }
    }
}
