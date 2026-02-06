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
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.ui.components.DailySummaryCard
import com.snaxlog.app.ui.components.DateNavigationBar
import com.snaxlog.app.ui.components.DeleteConfirmationDialog
import com.snaxlog.app.ui.components.EmptyStateView
import com.snaxlog.app.ui.components.FoodEntryCard
import com.snaxlog.app.ui.components.HistoricalDateBanner
import com.snaxlog.app.ui.components.MealCategoryHeader
import com.snaxlog.app.ui.components.SnaxlogDatePickerDialog
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.util.MealCategoryUtils
import kotlinx.coroutines.launch
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
            // FIP-EPIC-005: Stack TopAppBar and DateNavigationBar
            Column {
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
                // FIP-005: Group entries by meal category
                val groupedEntries = remember(uiState.entries) {
                    uiState.entries
                        .groupBy { it.entry.mealCategory }
                        .toSortedMap(compareBy { MealCategoryUtils.getCategorySortOrder(it) })
                }

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
                    // FIP-EPIC-005 US-014: Dynamic title based on selected date
                    item(key = "summary") {
                        val summaryTitle = if (uiState.isViewingToday) {
                            "Today's Summary"
                        } else {
                            val formatter = DateTimeFormatter.ofPattern("EEEE's Summary", Locale.getDefault())
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
                                    title = "No meals logged yet",
                                    message = "Start tracking by tapping the + button below"
                                )
                            } else {
                                EmptyStateView(
                                    title = "No meals logged",
                                    message = "No food entries were logged on this day. Tap the + button to add a retroactive entry."
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
