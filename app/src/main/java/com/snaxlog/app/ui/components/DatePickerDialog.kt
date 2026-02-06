package com.snaxlog.app.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * C-024: SnaxlogDatePickerDialog
 *
 * Full calendar picker for selecting any past date.
 * Part of FIP-EPIC-005: Historical Day Viewing (US-013).
 *
 * Features:
 * - Calendar view with month/year navigation
 * - Future dates visually disabled and not selectable (EC-095)
 * - Today clearly marked
 * - Supports date range constraints (no future dates)
 *
 * Uses Material3 DatePicker with SelectableDates constraint to block future dates.
 *
 * @param selectedDate Currently selected date
 * @param onDateSelected Callback when date is selected
 * @param onDismiss Callback when dialog is dismissed
 * @param maxDate Maximum selectable date (default: today) - EC-095, EC-123
 * @param modifier Compose modifier for customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnaxlogDatePickerDialog(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    maxDate: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
) {
    // Convert LocalDate to epoch milliseconds for DatePicker
    val initialSelectedDateMillis = selectedDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val maxDateMillis = maxDate
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Create date picker state with initial selection
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // EC-095: Prevent selection of future dates
                return utcTimeMillis <= maxDateMillis + (24 * 60 * 60 * 1000) // Add one day buffer for timezone issues
            }

            override fun isSelectableYear(year: Int): Boolean {
                // Allow all years up to current year
                return year <= maxDate.year
            }
        }
    )

    // Track if selected date is valid for confirm button
    val confirmEnabled = remember {
        derivedStateOf { datePickerState.selectedDateMillis != null }
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        // EC-123: Final validation to prevent future dates
                        if (selected <= maxDate) {
                            onDateSelected(selected)
                        } else {
                            // Fallback to max date if somehow a future date was selected
                            onDateSelected(maxDate)
                        }
                    }
                    onDismiss()
                },
                enabled = confirmEnabled.value
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier.semantics {
            contentDescription = "Select a date. Future dates are not available."
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = true,
            title = {
                Text(
                    text = "Select date",
                    modifier = Modifier.semantics {
                        contentDescription = "Select a date dialog. Currently selected: ${formatDateForAccessibility(selectedDate)}"
                    }
                )
            }
        )
    }
}

/**
 * Formats a date for accessibility announcements.
 *
 * @param date The date to format
 * @return Full date string for accessibility
 */
private fun formatDateForAccessibility(date: LocalDate): String {
    return "${date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${date.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${date.dayOfMonth}, ${date.year}"
}
