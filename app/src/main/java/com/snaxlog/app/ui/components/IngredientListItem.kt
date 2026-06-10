package com.snaxlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snaxlog.app.data.local.entity.ServingUnit
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * Data class representing an ingredient in the recipe editor.
 */
data class IngredientItem(
    val id: Long,
    val foodId: Long,
    val foodName: String,
    val quantity: Double,
    val unit: ServingUnit,
    val caloriesPerServing: Int,
    val proteinPerServing: Double,
    val fatPerServing: Double,
    val carbsPerServing: Double,
    /** Numeric serving size value for calculating total amount */
    val servingSizeValue: Double = 1.0,
    /** Human-readable serving description (e.g., "100g" or "1 cup") for display */
    val servingSizeDisplay: String = ""
) {
    /**
     * Calculate total serving amount for display (quantity × servingSizeValue).
     */
    val totalServingAmount: Double
        get() = quantity * servingSizeValue
    /**
     * Calculate total calories for this ingredient quantity.
     */
    val totalCalories: Int
        get() = (caloriesPerServing * quantity).roundToInt()

    /**
     * Calculate total protein for this ingredient quantity.
     */
    val totalProtein: Double
        get() = proteinPerServing * quantity

    /**
     * Calculate total fat for this ingredient quantity.
     */
    val totalFat: Double
        get() = fatPerServing * quantity

    /**
     * Calculate total carbs for this ingredient quantity.
     */
    val totalCarbs: Double
        get() = carbsPerServing * quantity
}

/**
 * C-031: IngredientListItem
 * EPIC-006: User-Created Foods and Recipes
 * US-019: Create Recipe with Multiple Ingredients
 *
 * Displays a single ingredient in a recipe with its quantity and calculated nutrition.
 * Includes drag handle for reordering and remove button.
 * The serving unit is fixed to the food's original unit - only quantity can be changed.
 *
 * @param ingredient The ingredient data to display.
 * @param index Display index (1-based) for ordering indication.
 * @param onQuantityChange Callback when quantity is changed.
 * @param onRemove Callback when remove button is clicked.
 * @param modifier Modifier for the component.
 * @param showDragHandle Whether to show the drag handle for reordering.
 */
@Composable
fun IngredientListItem(
    ingredient: IngredientItem,
    index: Int,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showDragHandle: Boolean = true
) {
    val numberFormat = NumberFormat.getNumberInstance()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.screenPadding,
                vertical = Spacing.sm
            )
            .semantics {
                contentDescription = "Ingredient $index: ${ingredient.foodName}, ${ingredient.quantity} ${ingredient.unit.displayName}"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Drag handle
        if (showDragHandle) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = Spacing.sm)
            )
        }

        // Index indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        // Ingredient info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = ingredient.foodName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Show total amount: quantity × servingSizeValue with unit
            // e.g., "2 servings × 100g = 200g" simplified to "200g"
            val displayText = if (ingredient.servingSizeDisplay.isNotEmpty()) {
                // For pre-loaded foods, show quantity × parsed serving or just "X servings of [serving]"
                if (ingredient.quantity == 1.0) {
                    ingredient.servingSizeDisplay
                } else {
                    "${ingredient.quantity.formatForDisplay()} × ${ingredient.servingSizeDisplay}"
                }
            } else {
                // For custom foods with numeric serving size
                val totalAmount = ingredient.totalServingAmount
                "${totalAmount.formatForDisplay()} ${ingredient.unit.abbreviation}"
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Nutrition summary
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = numberFormat.format(ingredient.totalCalories),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = " cal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier.padding(start = Spacing.xs)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove ${ingredient.foodName}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Compact version of ingredient list item for display-only purposes.
 * Used when viewing recipe details without edit capability.
 */
@Composable
fun IngredientListItemCompact(
    ingredient: IngredientItem,
    index: Int,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Spacing.screenPadding,
                vertical = Spacing.xs
            )
            .semantics {
                contentDescription = "${ingredient.foodName}, ${ingredient.quantity} ${ingredient.unit.displayName}, ${ingredient.totalCalories} calories"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$index.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
            )

            Text(
                text = ingredient.foodName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(Spacing.sm))

            // Show total amount with serving info
            val displayText = if (ingredient.servingSizeDisplay.isNotEmpty()) {
                if (ingredient.quantity == 1.0) {
                    "(${ingredient.servingSizeDisplay})"
                } else {
                    "(${ingredient.quantity.formatForDisplay()} × ${ingredient.servingSizeDisplay})"
                }
            } else {
                val totalAmount = ingredient.totalServingAmount
                "(${totalAmount.formatForDisplay()} ${ingredient.unit.abbreviation})"
            }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "${numberFormat.format(ingredient.totalCalories)} cal",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary
        )
    }
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
