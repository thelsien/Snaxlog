package com.snaxlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras

enum class MacroType {
    PROTEIN, FAT, CARBS
}

/**
 * C-005: MacroIndicator
 *
 * Displays a single macronutrient value with color-coded icon.
 * When a goal is set, shows value vs goal text and a mini progress bar
 * with color thresholds matching the calorie progress bar:
 * - 0-89%: success (green)
 * - 90-100%: warning (orange)
 * - 100%+: error (red)
 *
 * @param type The macro type (PROTEIN, FAT, or CARBS).
 * @param value The consumed amount in grams.
 * @param goal Optional goal amount in grams. When set, displays progress bar and goal comparison.
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun MacroIndicator(
    type: MacroType,
    value: Double,
    goal: Double? = null,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors

    val (icon, macroColor, label) = when (type) {
        MacroType.PROTEIN -> Triple(Icons.Filled.FitnessCenter, customColors.protein, "Protein")
        MacroType.FAT -> Triple(Icons.Filled.Opacity, customColors.fat, "Fat")
        MacroType.CARBS -> Triple(Icons.Filled.Grain, customColors.carbs, "Carbs")
    }

    val valueText = formatMacroValue(value)
    val displayText = if (goal != null && goal > 0) {
        "${valueText}g / ${formatMacroValue(goal)}g"
    } else {
        "${valueText}g"
    }

    // Calculate progress for macro when goal is set
    val macroProgress = if (goal != null && goal > 0) {
        (value / goal).toFloat()
    } else {
        null
    }

    val description = if (macroProgress != null) {
        val percentage = (macroProgress * 100).toInt().coerceAtLeast(0)
        "$label: $displayText ($percentage% of goal)"
    } else {
        "$label: $displayText"
    }

    Column(
        modifier = modifier.semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative, parent has description
            modifier = Modifier.size(24.dp),
            tint = macroColor
        )
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )

        // Show mini progress bar when a macro goal is set
        if (macroProgress != null) {
            Spacer(modifier = Modifier.height(4.dp))
            MacroProgressBar(
                progress = macroProgress,
                macroColor = macroColor,
                modifier = Modifier.width(64.dp)
            )
        }
    }
}

/**
 * Mini progress bar for individual macro indicators.
 *
 * Uses the same color threshold logic as the main calorie progress bar:
 * - 0-89%: success (green)
 * - 90-100%: warning (orange)
 * - 100%+: error (red)
 *
 * @param progress Value from 0.0 to any positive float. Visually capped at [PROGRESS_VISUAL_CAP].
 * @param macroColor The base color for the macro (used as track tint).
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun MacroProgressBar(
    progress: Float,
    macroColor: Color,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors

    val progressColor = getProgressColor(
        progress = progress,
        successColor = customColors.success,
        warningColor = customColors.warning,
        errorColor = MaterialTheme.colorScheme.error
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, PROGRESS_VISUAL_CAP),
        animationSpec = tween(durationMillis = 300),
        label = "macro_progress_animation"
    )

    LinearProgressIndicator(
        progress = { animatedProgress.coerceIn(0f, 1f) },
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = progressColor,
        trackColor = macroColor.copy(alpha = 0.2f),
    )
}

private fun formatMacroValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
