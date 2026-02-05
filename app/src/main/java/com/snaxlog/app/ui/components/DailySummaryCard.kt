package com.snaxlog.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import java.text.NumberFormat

/**
 * C-003: DailySummaryCard
 *
 * Prominent card showing daily calorie and macro totals vs. goal.
 * Changes appearance based on goal progress with visual indicators:
 *
 * Variants:
 * - default: Within goal (0-89%) - Success color (green)
 * - approaching: Near goal (90-100%) - Warning color (orange) + "approaching limit" text
 * - exceeded: Over goal (100%+) - Error color (red) + "over goal by X" text
 * - noGoal: No active goal set - Totals only with "Tap to set a goal" prompt
 *
 * Uses the reusable ProgressBar (C-004) and MacroIndicator (C-005) components.
 */
@Composable
fun DailySummaryCard(
    caloriesConsumed: Int,
    calorieGoal: Int?,
    proteinConsumed: Double,
    fatConsumed: Double,
    carbsConsumed: Double,
    proteinGoal: Double? = null,
    fatGoal: Double? = null,
    carbsGoal: Double? = null,
    onGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors
    val numberFormat = NumberFormat.getNumberInstance()

    // Calculate calorie progress (AC-052, AC-055)
    val progress = if (calorieGoal != null && calorieGoal > 0) {
        caloriesConsumed.toFloat() / calorieGoal.toFloat()
    } else 0f

    val remaining = if (calorieGoal != null) calorieGoal - caloriesConsumed else null

    // Build accessibility description
    val descriptionText = buildAccessibilityDescription(
        caloriesConsumed = caloriesConsumed,
        calorieGoal = calorieGoal,
        remaining = remaining,
        progress = progress,
        proteinConsumed = proteinConsumed,
        fatConsumed = fatConsumed,
        carbsConsumed = carbsConsumed,
        numberFormat = numberFormat
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.cardMarginHorizontal, vertical = Spacing.sm)
            .clickable(onClick = onGoalClick)
            .semantics { contentDescription = descriptionText },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Summary",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(onClick = onGoalClick) {
                    Text(
                        text = if (calorieGoal != null) "Goals" else "Set Goal",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Calorie numbers (AC-052)
            if (calorieGoal != null) {
                Text(
                    text = "${numberFormat.format(caloriesConsumed)} / ${numberFormat.format(calorieGoal)}",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = numberFormat.format(caloriesConsumed),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "calories",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )

            // Progress bar and remaining text - only shown when goal exists
            if (calorieGoal != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                // C-004: ProgressBar with color thresholds
                ProgressBar(progress = progress)

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Remaining/status text with appropriate coloring (AC-055, EC-087)
                val (remainingText, remainingColor) = getRemainingTextAndColor(
                    remaining = remaining,
                    progress = progress,
                    numberFormat = numberFormat,
                    successColor = customColors.success,
                    warningColor = customColors.warning,
                    errorColor = MaterialTheme.colorScheme.error,
                    defaultColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = remainingColor,
                    textAlign = TextAlign.Center
                )
            } else {
                // STATE-003: No Goal (AC-056)
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Tap to set a goal for tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Macro row (AC-053) with progress bars when goals are set
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroIndicator(
                    type = MacroType.PROTEIN,
                    value = proteinConsumed,
                    goal = proteinGoal
                )
                MacroIndicator(
                    type = MacroType.FAT,
                    value = fatConsumed,
                    goal = fatGoal
                )
                MacroIndicator(
                    type = MacroType.CARBS,
                    value = carbsConsumed,
                    goal = carbsGoal
                )
            }
        }
    }
}

/**
 * Determines the remaining text and its color based on the current progress state.
 *
 * @return Pair of (text, color) for the remaining/status indicator.
 */
internal fun getRemainingTextAndColor(
    remaining: Int?,
    progress: Float,
    numberFormat: NumberFormat,
    successColor: androidx.compose.ui.graphics.Color,
    warningColor: androidx.compose.ui.graphics.Color,
    errorColor: androidx.compose.ui.graphics.Color,
    defaultColor: androidx.compose.ui.graphics.Color
): Pair<String, androidx.compose.ui.graphics.Color> {
    if (remaining == null) return Pair("", defaultColor)

    return when {
        // EC-087: Exactly 100% shows completion indicator
        remaining == 0 -> Pair("Goal reached!", successColor)
        // AC-055: Over goal shows error indicator
        remaining < 0 -> Pair(
            "Over goal by ${numberFormat.format(-remaining)}",
            errorColor
        )
        // 90-100%: Approaching goal warning
        progress >= PROGRESS_THRESHOLD_WARNING -> Pair(
            "${numberFormat.format(remaining)} remaining - approaching limit",
            warningColor
        )
        // 0-89%: Normal remaining
        else -> Pair(
            "${numberFormat.format(remaining)} remaining",
            defaultColor
        )
    }
}

/**
 * Builds the accessibility content description for the daily summary card.
 */
private fun buildAccessibilityDescription(
    caloriesConsumed: Int,
    calorieGoal: Int?,
    remaining: Int?,
    progress: Float,
    proteinConsumed: Double,
    fatConsumed: Double,
    carbsConsumed: Double,
    numberFormat: NumberFormat
): String {
    return if (calorieGoal != null) {
        val percentage = (progress * 100).toInt().coerceAtLeast(0)
        val progressStatus = when {
            progress >= PROGRESS_THRESHOLD_EXCEEDED -> "Over goal."
            progress >= PROGRESS_THRESHOLD_WARNING -> "Approaching goal limit."
            else -> ""
        }
        "Daily summary. ${numberFormat.format(caloriesConsumed)} of ${numberFormat.format(calorieGoal)} calories consumed. " +
                "$percentage percent of goal. $progressStatus " +
                "${remaining?.let { if (it >= 0) "${numberFormat.format(it)} remaining" else "Over by ${numberFormat.format(-it)}" } ?: ""}. " +
                "Protein: ${formatMacro(proteinConsumed)}g, Fat: ${formatMacro(fatConsumed)}g, Carbs: ${formatMacro(carbsConsumed)}g. " +
                "Tap to manage goals."
    } else {
        "Daily summary. ${numberFormat.format(caloriesConsumed)} calories consumed. No goal set. Tap to set a goal."
    }
}

private fun formatMacro(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
