package com.snaxlog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat

/**
 * C-016: NutritionPreview
 * Live preview of calculated nutrition based on food selection and serving size.
 */
@Composable
fun NutritionPreview(
    totalCalories: Int,
    totalProtein: Double,
    totalFat: Double,
    totalCarbs: Double,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance()
    val description = "${numberFormat.format(totalCalories)} calories. " +
            "Protein: ${formatValue(totalProtein)}g, Fat: ${formatValue(totalFat)}g, Carbs: ${formatValue(totalCarbs)}g"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Nutrition preview. $description" },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(Spacing.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Nutrition Preview",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = "${numberFormat.format(totalCalories)} calories",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroIndicator(type = MacroType.PROTEIN, value = totalProtein)
                MacroIndicator(type = MacroType.FAT, value = totalFat)
                MacroIndicator(type = MacroType.CARBS, value = totalCarbs)
            }
        }
    }
}

private fun formatValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
