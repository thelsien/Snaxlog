package com.snaxlog.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras

enum class MacroType {
    PROTEIN, FAT, CARBS
}

/**
 * C-005: MacroIndicator
 * Displays a single macronutrient value with color-coded icon.
 */
@Composable
fun MacroIndicator(
    type: MacroType,
    value: Double,
    goal: Double? = null,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors

    val (icon, color, label) = when (type) {
        MacroType.PROTEIN -> Triple(Icons.Filled.FitnessCenter, customColors.protein, "Protein")
        MacroType.FAT -> Triple(Icons.Filled.Opacity, customColors.fat, "Fat")
        MacroType.CARBS -> Triple(Icons.Filled.Grain, customColors.carbs, "Carbs")
    }

    val valueText = formatMacroValue(value)
    val displayText = if (goal != null) {
        "${valueText}g / ${formatMacroValue(goal)}g"
    } else {
        "${valueText}g"
    }

    val description = "$label: $displayText"

    Column(
        modifier = modifier.semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null, // decorative, parent has description
            modifier = Modifier.size(24.dp),
            tint = color
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
    }
}

private fun formatMacroValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
