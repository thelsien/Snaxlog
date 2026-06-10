package com.snaxlog.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * C-029: CalorieCalculator
 * EPIC-006: User-Created Foods and Recipes
 * US-018 AC-013-002: Auto-calculated calories displayed in real-time
 *
 * Displays auto-calculated calories based on macro nutrients.
 * Shows the calculation formula and updates in real-time as macros change.
 *
 * Formula: Calories = (Protein x 4) + (Carbs x 4) + (Fat x 9)
 *
 * @param protein Protein in grams.
 * @param fat Fat in grams.
 * @param carbs Carbs in grams.
 * @param modifier Modifier for the component.
 * @param showFormula Whether to show the calculation formula breakdown.
 */
@Composable
fun CalorieCalculator(
    protein: Double,
    fat: Double,
    carbs: Double,
    modifier: Modifier = Modifier,
    showFormula: Boolean = true
) {
    val proteinCalories = (protein * 4).roundToInt()
    val fatCalories = (fat * 9).roundToInt()
    val carbsCalories = (carbs * 4).roundToInt()
    // Round the exact total rather than summing rounded parts, so the
    // displayed total always matches the value persisted on save
    val totalCalories = FoodEntity.calculateCalories(protein, fat, carbs)

    val numberFormat = NumberFormat.getNumberInstance()
    val caloriesText = numberFormat.format(totalCalories)

    val description = "$totalCalories calories calculated from ${protein}g protein, ${fat}g fat, ${carbs}g carbs"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(Spacing.base)
            .animateContentSize()
            .semantics {
                contentDescription = description
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = Spacing.sm)
                )
                Text(
                    text = "Calculated Calories",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = caloriesText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = " cal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        if (showFormula) {
            Text(
                text = buildFormulaString(protein, fat, carbs, proteinCalories, fatCalories, carbsCalories),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sm)
            )
        }
    }
}

/**
 * Builds the formula breakdown string.
 */
private fun buildFormulaString(
    protein: Double,
    fat: Double,
    carbs: Double,
    proteinCal: Int,
    fatCal: Int,
    carbsCal: Int
): String {
    val proteinStr = formatMacro(protein)
    val fatStr = formatMacro(fat)
    val carbsStr = formatMacro(carbs)

    return "(${proteinStr}g protein x 4 = $proteinCal) + " +
            "(${carbsStr}g carbs x 4 = $carbsCal) + " +
            "(${fatStr}g fat x 9 = $fatCal)"
}

/**
 * Formats a macro value for display.
 */
private fun formatMacro(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
