package com.snaxlog.app.ui.screens.dailyfoodlog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.snaxlog.app.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.snaxlog.app.ui.components.DailySummaryCard
import com.snaxlog.app.ui.components.DateNavigationBar
import com.snaxlog.app.ui.components.DeleteConfirmationDialog
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.FoodEntryCard
import com.snaxlog.app.ui.components.DateSwipeContainer
import com.snaxlog.app.ui.components.HistoricalDateBanner
import com.snaxlog.app.ui.components.MealCategoryHeader
import com.snaxlog.app.ui.components.SnaxlogDatePickerDialog
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.util.MealCategoryUtils
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * S-001: DailyFoodLogScreen
 * Main app screen showing daily food log and summary.
 * FIP-005: Now displays entries grouped by meal category with headers.
 * FIP-EPIC-005: Now supports historical day viewing with date navigation (US-013 to US-017).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyFoodLogScreen(
    viewModel: DailyFoodLogViewModel,
    onNavigateToGoals: () -> Unit = {},
    onNavigateToCustomFoods: () -> Unit = {},
    addFoodViewModel: AddFoodViewModel = hiltViewModel(),
    editFoodViewModel: EditFoodViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val addFoodState by addFoodViewModel.addFoodState.collectAsStateWithLifecycle()
    val editFoodState by editFoodViewModel.editFoodState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val addFoodEntryDescription = stringResource(R.string.daily_log_add_food)

    // Bottom sheet states
    var showAddFoodSheet by remember { mutableStateOf(false) }
    var showEditFoodSheet by remember { mutableStateOf(false) }

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
        }
    }

    // Add-food result: close the sheet and surface the "Entry added" snackbar on the main screen.
    // The literal is kept in sync with the AddFoodViewModel/snackbar contract.
    LaunchedEffect(addFoodState.saveSuccess, addFoodState.saveError) {
        if (addFoodState.saveSuccess) {
            showAddFoodSheet = false
            viewModel.showSnackbar("Entry added")
            addFoodViewModel.onSaveHandled()
        } else if (addFoodState.saveError) {
            viewModel.showError("Failed to save entry. Please try again.")
            addFoodViewModel.onSaveHandled()
        }
    }

    // Edit-food result: close the sheet and surface the "Entry updated" snackbar on the main screen.
    LaunchedEffect(editFoodState.saveSuccess, editFoodState.saveError) {
        if (editFoodState.saveSuccess) {
            showEditFoodSheet = false
            viewModel.showSnackbar("Entry updated")
            editFoodViewModel.onSaveHandled()
        } else if (editFoodState.saveError) {
            viewModel.showError("Failed to update entry. Please try again.")
            editFoodViewModel.onSaveHandled()
        }
    }

    Scaffold(
        topBar = {
            // FIP-EPIC-005: Stack TopAppBar and DateNavigationBar
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.daily_log_app_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        val myFoodsDescription = stringResource(R.string.daily_log_my_foods)
                        IconButton(
                            onClick = onNavigateToCustomFoods,
                            modifier = Modifier.semantics {
                                contentDescription = myFoodsDescription
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // FIP-EPIC-005 US-013: DateNavigationBar (C-023)
                DateNavigationBar(
                    selectedDate = uiState.selectedDate,
                    onDateChange = { viewModel.setSelectedDate(it) },
                    onOpenDatePicker = { viewModel.openDatePicker() }
                )

                // FIP-EPIC-005 US-013: HistoricalDateBanner (C-025) - only visible when not viewing today
                AnimatedVisibility(
                    visible = !uiState.isViewingToday,
                    enter = fadeIn(animationSpec = tween(200)) +
                            slideInVertically(animationSpec = tween(200)) { -it },
                    exit = fadeOut(animationSpec = tween(200)) +
                           slideOutVertically(animationSpec = tween(200)) { -it }
                ) {
                    HistoricalDateBanner(
                        date = uiState.selectedDate,
                        onReturnToToday = { viewModel.returnToToday() }
                    )
                }
            }
        },
        floatingActionButton = {
            if (!uiState.isLoading && uiState.error == null) {
                FloatingActionButton(
                    onClick = {
                        // Pass the currently selected date explicitly so entries log to that day (US-017).
                        addFoodViewModel.openAddFood(viewModel.currentSelectedDate())
                        showAddFoodSheet = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.semantics {
                        contentDescription = addFoodEntryDescription
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
                        text = uiState.error ?: stringResource(R.string.common_unknown_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                // FIP-005: Group entries by meal category
                val groupedEntries = remember(uiState.entries) {
                    uiState.entries
                        .groupBy { it.entry.mealCategory }
                        .toSortedMap(compareBy { MealCategoryUtils.getCategorySortOrder(it) })
                }

                // FIP-EPIC-005: Wrap content in DateSwipeContainer for swipe navigation
                DateSwipeContainer(
                    onNavigateToPrevious = { viewModel.navigateToPreviousDay() },
                    onNavigateToNext = { viewModel.navigateToNextDay() },
                    canNavigateToNext = uiState.canNavigateForward,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = Spacing.sm,
                            bottom = Spacing.fabClearance
                        )
                    ) {
                    // Summary card - always shown
                    // FIP-EPIC-005 US-014: Dynamic title based on selected date
                    item(key = "summary") {
                        val summaryTitle = if (uiState.isViewingToday) {
                            stringResource(R.string.daily_log_today_summary)
                        } else {
                            val formatter = DateTimeFormatter.ofPattern("EEEE'''s Summary'", Locale.getDefault())
                            uiState.selectedDate.format(formatter)
                        }

                        DailySummaryCard(
                            title = summaryTitle,
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
                        // FIP-EPIC-005 US-014: Different empty state for historical dates
                        item(key = "empty") {
                            if (uiState.isViewingToday) {
                                EmptyStateView(
                                    title = stringResource(R.string.daily_log_empty_today_title),
                                    message = stringResource(R.string.daily_log_empty_today_message)
                                )
                            } else {
                                EmptyStateView(
                                    title = stringResource(R.string.daily_log_empty_historical_title),
                                    message = stringResource(R.string.daily_log_empty_historical_message)
                                )
                            }
                        }
                    } else {
                        // FIP-005: Display entries grouped by meal category with sticky headers
                        groupedEntries.forEach { (category, categoryEntries) ->
                            // Calculate subtotals for this category
                            val totalCals = categoryEntries.sumOf { it.entry.totalCalories }
                            val totalProt = categoryEntries.sumOf { it.entry.totalProtein }
                            val totalFatVal = categoryEntries.sumOf { it.entry.totalFat }
                            val totalCarbsVal = categoryEntries.sumOf { it.entry.totalCarbs }

                            // Sticky header for category
                            stickyHeader(key = "header_${category?.name ?: "uncategorized"}") {
                                MealCategoryHeader(
                                    category = category,
                                    entryCount = categoryEntries.size,
                                    totalCalories = totalCals,
                                    totalProtein = totalProt,
                                    totalFat = totalFatVal,
                                    totalCarbs = totalCarbsVal
                                )
                            }

                            // Entries within category (sorted by timestamp descending)
                            val sortedEntries = categoryEntries.sortedByDescending { it.entry.timestamp }
                            items(
                                items = sortedEntries,
                                key = { it.entry.id }
                            ) { entryWithFood ->
                                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val timeStr = timeFormat.format(Date(entryWithFood.entry.timestamp))
                                val servingsText = stringResource(
                                    R.string.daily_log_servings_display,
                                    formatServingsCount(entryWithFood.entry.servings),
                                    entryWithFood.food.servingSize
                                )

                                FoodEntryCard(
                                    foodName = entryWithFood.food.name,
                                    servingSize = servingsText,
                                    calories = entryWithFood.entry.totalCalories,
                                    timestamp = timeStr,
                                    onTap = {
                                        editFoodViewModel.loadEntryForEdit(entryWithFood.entry.id)
                                        showEditFoodSheet = true
                                    },
                                    onDelete = {
                                        viewModel.showDeleteDialog(entryWithFood)
                                    },
                                    // FIP-005: Pass meal category for badge display
                                    mealCategory = entryWithFood.entry.mealCategory
                                )
                            }
                        }
                    }
                    }
                }
            }
        }

        // Delete confirmation dialog
        uiState.deleteDialogEntry?.let {
            DeleteConfirmationDialog(
                title = stringResource(R.string.daily_log_delete_title),
                message = stringResource(R.string.daily_log_delete_message),
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
                    viewModel = addFoodViewModel,
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
                    viewModel = editFoodViewModel,
                    onDismiss = { showEditFoodSheet = false }
                )
            }
        }

        // FIP-EPIC-005 US-013: Date picker dialog (C-024)
        if (uiState.isDatePickerOpen) {
            SnaxlogDatePickerDialog(
                selectedDate = uiState.selectedDate,
                onDateSelected = { viewModel.onDatePickerDateSelected(it) },
                onDismiss = { viewModel.closeDatePicker() }
            )
        }
    }
}

private fun formatServingsCount(servings: Double): String {
    return if (servings == servings.toLong().toDouble()) {
        "${servings.toLong()}.0"
    } else {
        String.format(Locale.getDefault(), "%.2f", servings).trimEnd('0').let {
            if (it.endsWith(".")) "${it}0" else it
        }
    }
}
