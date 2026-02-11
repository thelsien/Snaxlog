package com.snaxlog.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.CarbsDark
import com.snaxlog.app.ui.theme.CarbsLight
import com.snaxlog.app.ui.theme.FatDark
import com.snaxlog.app.ui.theme.FatLight
import com.snaxlog.app.ui.theme.ProteinDark
import com.snaxlog.app.ui.theme.ProteinLight
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat

/**
 * C-032: RecipeNutritionSummary
 * EPIC-006: User-Created Foods and Recipes
 * US-019 AC-014-003: Real-time nutrition summary for recipe
 *
 * Displays the total and per-serving nutrition calculated from recipe ingredients.
 * Updates in real-time as ingredients are added, removed, or quantities changed.
 *
 * @param totalCalories Total calories from all ingredients.
 * @param totalProtein Total protein from all ingredients.
 * @param totalFat Total fat from all ingredients.
 * @param totalCarbs Total carbs from all ingredients.
 * @param numberOfServings Number of servings the recipe makes.
 * @param ingredientCount Number of ingredients in the recipe.
 * @param modifier Modifier for the component.
 */
@Composable
fun RecipeNutritionSummary(
    totalCalories: Int,
    totalProtein: Double,
    totalFat: Double,
    totalCarbs: Double,
    numberOfServings: Double,
    ingredientCount: Int,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val numberFormat = NumberFormat.getNumberInstance()

    // Calculate per-serving values
    val perServingCalories = if (numberOfServings > 0) (totalCalories / numberOfServings).toInt() else 0
    val perServingProtein = if (numberOfServings > 0) totalProtein / numberOfServings else 0.0
    val perServingFat = if (numberOfServings > 0) totalFat / numberOfServings else 0.0
    val perServingCarbs = if (numberOfServings > 0) totalCarbs / numberOfServings else 0.0

    val description = "Recipe nutrition: $totalCalories total calories, $perServingCalories per serving. " +
            "$ingredientCount ingredients making ${numberOfServings.formatForDisplay()} servings."

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(Spacing.base)
            .animateContentSize()
            .semantics {
                contentDescription = description
            }
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = Spacing.sm)
                )
                Text(
                    text = "Recipe Nutrition",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "$ingredientCount ${if (ingredientCount == 1) "ingredient" else "ingredients"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Per Serving Section
        Text(
            text = "Per Serving (makes ${numberOfServings.formatForDisplay()} servings)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Per-serving calories (large)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = numberFormat.format(perServingCalories),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = " calories",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Per-serving macros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroColumn(
                label = "Protein",
                value = perServingProtein,
                color = if (isDarkTheme) ProteinDark else ProteinLight
            )
            MacroColumn(
                label = "Fat",
                value = perServingFat,
                color = if (isDarkTheme) FatDark else FatLight
            )
            MacroColumn(
                label = "Carbs",
                value = perServingCarbs,
                color = if (isDarkTheme) CarbsDark else CarbsLight
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(Spacing.sm))

        // Total Section
        Text(
            text = "Total Recipe",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${numberFormat.format(totalCalories)} cal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "P: ${totalProtein.formatForDisplay()}g | F: ${totalFat.formatForDisplay()}g | C: ${totalCarbs.formatForDisplay()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Column displaying a single macro nutrient.
 */
@Composable
private fun MacroColumn(
    label: String,
    value: Double,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = value.formatForDisplay(),
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = "g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Extension function to calculate luminance of a color.
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

/**
 * Extension to format double values for display.
 */
private fun Double.formatForDisplay(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        String.format("%.1f", this)
    }
}
