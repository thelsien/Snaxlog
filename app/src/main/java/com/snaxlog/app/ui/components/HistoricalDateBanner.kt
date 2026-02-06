package com.snaxlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import com.snaxlog.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * C-025: HistoricalDateBanner
 *
 * Persistent banner shown when viewing a historical date, with quick return-to-today action.
 * Part of FIP-EPIC-005: Historical Day Viewing (US-013, US-014).
 *
 * Features:
 * - Appears below date navigation bar when not viewing today
 * - Shows "Viewing [Date]" with history icon
 * - "Return to Today" action button
 * - Subtle background to distinguish from content
 * - Animates in/out on date change
 *
 * Accessibility:
 * - Role: Status
 * - Live region: Polite (announces when appearing/disappearing)
 * - Content description includes full date and action instructions
 *
 * @param date The historical date being viewed
 * @param onReturnToToday Callback when return to today is tapped
 * @param modifier Compose modifier for customization
 */
@Composable
fun HistoricalDateBanner(
    date: LocalDate,
    onReturnToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors

    // Format the date for display
    val formattedDate = formatBannerDate(date)
    val fullDateDescription = formatFullDateForAccessibility(date)

    val bannerDescription = "Viewing historical date: $fullDateDescription. Tap 'Return to Today' to go back to current day."

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(customColors.historicalDateBackground)
            .padding(horizontal = Spacing.base)
            .semantics {
                contentDescription = bannerDescription
                liveRegion = LiveRegionMode.Polite
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: History icon + date text
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = customColors.historicalDate
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "Viewing $formattedDate",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Right side: Return to Today button
        TextButton(
            onClick = onReturnToToday,
            modifier = Modifier.semantics {
                contentDescription = "Return to today"
            }
        ) {
            Text(
                text = "Return to Today",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Formats the date for the banner display.
 * Format: "Tuesday, February 4, 2026"
 *
 * @param date The date to format
 * @return Formatted date string
 */
private fun formatBannerDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    return date.format(formatter)
}

/**
 * Formats the date for accessibility announcements.
 *
 * @param date The date to format
 * @return Full date string for accessibility
 */
private fun formatFullDateForAccessibility(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())
    return date.format(formatter)
}
