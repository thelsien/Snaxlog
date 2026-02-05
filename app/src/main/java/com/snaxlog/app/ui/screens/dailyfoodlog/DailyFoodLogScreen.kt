package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.snaxlog.app.ui.components.DailySummaryCard
import com.snaxlog.app.ui.components.DeleteConfirmationDialog
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.FoodEntryCard
import com.snaxlog.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * S-001: DailyFoodLogScreen
 * Main app screen showing daily food log and summary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyFoodLogScreen(
    viewModel: DailyFoodLogViewModel,
    onNavigateToGoals: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Bottom sheet states
    var showAddFoodSheet by remember { mutableStateOf(false) }
    var showEditFoodSheet by remember { mutableStateOf(false) }
    var editingEntryId by remember { mutableStateOf<Long?>(null) }

    val addFoodSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editFoodSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // EC-005: Refresh date when app resumes from background
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshDate()
        }
    }

    // Handle snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()

            // Close sheets after successful operation
            if (message == "Entry added") {
                showAddFoodSheet = false
            }
            if (message == "Entry updated") {
                showEditFoodSheet = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Snaxlog",
                        style = MaterialTheme.typography.titleLarge
                    )
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
                        viewModel.openAddFood()
                        showAddFoodSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = "Add food entry"
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        top = Spacing.sm,
                        bottom = Spacing.fabClearance
                    )
                ) {
                    // Summary card - always shown
                    item(key = "summary") {
                        DailySummaryCard(
                            caloriesConsumed = uiState.totalCalories,
                            calorieGoal = uiState.activeGoal?.calorieTarget,
                            proteinConsumed = uiState.totalProtein,
                            fatConsumed = uiState.totalFat,
                            carbsConsumed = uiState.totalCarbs,
                            proteinGoal = uiState.activeGoal?.proteinTarget,
                            fatGoal = uiState.activeGoal?.fatTarget,
                            carbsGoal = uiState.activeGoal?.carbsTarget,
                            onGoalClick = onNavigateToGoals
                        )
                    }

                    if (uiState.entries.isEmpty()) {
                        item(key = "empty") {
                            EmptyStateView(
                                title = "No meals logged yet",
                                message = "Start tracking by tapping the + button below"
                            )
                        }
                    } else {
                        items(
                            items = uiState.entries,
                            key = { it.entry.id }
                        ) { entryWithFood ->
                            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                            val timeStr = timeFormat.format(Date(entryWithFood.entry.timestamp))
                            val servingsText = formatServingsDisplay(
                                entryWithFood.entry.servings,
                                entryWithFood.food.servingSize
                            )

                            FoodEntryCard(
                                foodName = entryWithFood.food.name,
                                servingSize = servingsText,
                                calories = entryWithFood.entry.totalCalories,
                                timestamp = timeStr,
                                onTap = {
                                    editingEntryId = entryWithFood.entry.id
                                    viewModel.loadEntryForEdit(entryWithFood.entry.id)
                                    showEditFoodSheet = true
                                },
                                onDelete = {
                                    viewModel.showDeleteDialog(entryWithFood)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        uiState.deleteDialogEntry?.let {
            DeleteConfirmationDialog(
                title = "Delete entry?",
                message = "This entry will be permanently removed from your log.",
                onConfirm = { viewModel.confirmDeleteEntry() },
                onDismiss = { viewModel.dismissDeleteDialog() }
            )
        }

        // Add food bottom sheet
        if (showAddFoodSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddFoodSheet = false },
                sheetState = addFoodSheetState
            ) {
                AddFoodSheetContent(
                    viewModel = viewModel,
                    onDismiss = { showAddFoodSheet = false }
                )
            }
        }

        // Edit food bottom sheet
        if (showEditFoodSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditFoodSheet = false },
                sheetState = editFoodSheetState
            ) {
                EditFoodSheetContent(
                    viewModel = viewModel,
                    onDismiss = { showEditFoodSheet = false }
                )
            }
        }
    }
}

private fun formatServingsDisplay(servings: Double, servingSize: String): String {
    val servingsStr = if (servings == servings.toLong().toDouble()) {
        "${servings.toLong()}.0"
    } else {
        String.format("%.2f", servings).trimEnd('0').let {
            if (it.endsWith(".")) "${it}0" else it
        }
    }
    return "$servingsStr servings ($servingSize)"
}
