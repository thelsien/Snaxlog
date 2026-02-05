package com.snaxlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras
import java.text.NumberFormat

/**
 * C-003: DailySummaryCard
 * Prominent card showing daily calorie and macro totals vs. goal.
 * Changes appearance based on goal progress.
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

    val progress = if (calorieGoal != null && calorieGoal > 0) {
        caloriesConsumed.toFloat() / calorieGoal.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1.5f),
        animationSpec = tween(durationMillis = 300),
        label = "progress"
    )

    val progressColor = when {
        progress < 0.9f -> customColors.success
        progress <= 1.0f -> customColors.warning
        else -> MaterialTheme.colorScheme.error
    }

    val remaining = if (calorieGoal != null) calorieGoal - caloriesConsumed else null

    val descriptionText = if (calorieGoal != null) {
        "Daily summary. ${numberFormat.format(caloriesConsumed)} of ${numberFormat.format(calorieGoal)} calories consumed. " +
                "${remaining?.let { "${numberFormat.format(it)} remaining" } ?: ""}. " +
                "Protein: ${formatMacro(proteinConsumed)}g, Fat: ${formatMacro(fatConsumed)}g, Carbs: ${formatMacro(carbsConsumed)}g. " +
                "Tap to manage goals."
    } else {
        "Daily summary. ${numberFormat.format(caloriesConsumed)} calories consumed. No goal set. Tap to set a goal."
    }

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

            // Calorie numbers
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

            // Progress bar - only shown when goal exists
            if (calorieGoal != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))

                LinearProgressIndicator(
                    progress = { animatedProgress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Remaining text
                val remainingText = when {
                    remaining == null -> ""
                    remaining > 0 -> "${numberFormat.format(remaining)} remaining"
                    remaining == 0 -> "Goal reached!"
                    else -> "Over goal by ${numberFormat.format(-remaining)}"
                }
                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (remaining != null && remaining < 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Tap to set a goal for tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            // Macro row
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

private fun formatMacro(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
