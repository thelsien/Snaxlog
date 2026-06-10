package com.snaxlog.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.snaxlog.app.R
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import com.snaxlog.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * C-023: DateNavigationBar
 *
 * Horizontal bar for navigating between dates with arrow buttons, date display, and today button.
 * Part of FIP-EPIC-005: Historical Day Viewing (US-013).
 *
 * Features:
 * - Displays current viewing date with tap-to-open date picker
 * - Left/right arrow buttons for single-day navigation
 * - "Today" quick-return button when viewing historical date
 * - Visual differentiation between "today" and historical dates
 *
 * Visual states:
 * - Viewing Today: Date in primary color (bold), next arrow disabled, no Today button
 * - Viewing Historical: Date in historical color, all navigation enabled, Today button visible
 *
 * @param selectedDate Currently displayed/selected date
 * @param onDateChange Callback when date changes via arrows
 * @param onOpenDatePicker Callback to open full date picker dialog
 * @param modifier Compose modifier for customization
 */
@Composable
fun DateNavigationBar(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isViewingToday = selectedDate == today
    val canNavigateForward = selectedDate < today

    val customColors = SnaxlogThemeExtras.customColors
    val context = LocalContext.current

    // Animate date text color based on viewing today or historical
    val dateColor by animateColorAsState(
        targetValue = if (isViewingToday) {
            MaterialTheme.colorScheme.primary
        } else {
            customColors.historicalDate
        },
        animationSpec = tween(durationMillis = 200),
        label = "date_color_animation"
    )

    // Animate next arrow alpha (disabled when viewing today)
    val nextArrowAlpha by animateFloatAsState(
        targetValue = if (canNavigateForward) 1f else 0.38f,
        animationSpec = tween(durationMillis = 150),
        label = "next_arrow_alpha"
    )

    // Format date display
    val dateDisplayText = formatDateDisplay(context, selectedDate, isViewingToday)

    // Build accessibility descriptions
    val previousDayDescription = stringResource(
        R.string.date_nav_previous_day,
        formatDateForAccessibility(selectedDate.minusDays(1))
    )
    val nextDayDescription = if (canNavigateForward) {
        stringResource(
            R.string.date_nav_next_day,
            formatDateForAccessibility(selectedDate.plusDays(1))
        )
    } else {
        stringResource(R.string.date_nav_cannot_navigate_forward)
    }
    val dateAreaDescription = stringResource(R.string.date_nav_open_calendar, dateDisplayText)
    val navigationBarDescription = stringResource(R.string.date_nav_bar_description, dateDisplayText)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = navigationBarDescription
            },
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = Spacing.base, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Previous Arrow Button (always enabled)
            IconButton(
                onClick = { onDateChange(selectedDate.minusDays(1)) },
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = previousDayDescription }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Date Display (clickable to open picker)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDatePicker)
                    .padding(horizontal = Spacing.sm)
                    .semantics { contentDescription = dateAreaDescription },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = dateColor
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = dateDisplayText,
                    style = MaterialTheme.typography.titleMedium,
                    color = dateColor,
                    fontWeight = if (isViewingToday) FontWeight.Bold else FontWeight.Normal
                )
            }

            // Next Arrow Button (disabled when viewing today)
            IconButton(
                onClick = {
                    if (canNavigateForward) {
                        onDateChange(selectedDate.plusDays(1))
                    }
                },
                enabled = canNavigateForward,
                modifier = Modifier
                    .size(48.dp)
                    .alpha(nextArrowAlpha)
                    .semantics { contentDescription = nextDayDescription }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Today Button (only visible when viewing historical date)
            AnimatedVisibility(
                visible = !isViewingToday,
                enter = fadeIn(animationSpec = tween(200)) +
                        slideInHorizontally(animationSpec = tween(200)) { it },
                exit = fadeOut(animationSpec = tween(200)) +
                       slideOutHorizontally(animationSpec = tween(200)) { it }
            ) {
                val returnToTodayDescription = stringResource(R.string.date_nav_return_to_today)
                TextButton(
                    onClick = { onDateChange(today) },
                    modifier = Modifier
                        .padding(start = Spacing.sm)
                        .semantics { contentDescription = returnToTodayDescription }
                ) {
                    Text(
                        text = stringResource(R.string.date_nav_today),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Formats the date for display in the navigation bar.
 *
 * Display formats:
 * - Today: "Today, February 6, 2026"
 * - Yesterday: "Yesterday, February 5, 2026" (optional, currently shows full day name)
 * - Other: "Thursday, February 4, 2026"
 *
 * @param date The date to format
 * @param isToday Whether the date is today
 * @return Formatted date string
 */
private fun formatDateDisplay(context: android.content.Context, date: LocalDate, isToday: Boolean): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    val formattedDate = date.format(formatter)

    return if (isToday) {
        context.getString(R.string.date_nav_today_prefix, formattedDate.substringAfter(", "))
    } else {
        formattedDate
    }
}

/**
 * Formats a date for screen reader accessibility announcements.
 *
 * @param date The date to format
 * @return Full date string for accessibility
 */
private fun formatDateForAccessibility(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    return date.format(formatter)
}
