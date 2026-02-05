package com.snaxlog.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.ui.theme.BreakfastDark
import com.snaxlog.app.ui.theme.BreakfastLight
import com.snaxlog.app.ui.theme.DinnerDark
import com.snaxlog.app.ui.theme.DinnerLight
import com.snaxlog.app.ui.theme.LunchDark
import com.snaxlog.app.ui.theme.LunchLight
import com.snaxlog.app.ui.theme.SnackingDark
import com.snaxlog.app.ui.theme.SnackingLight
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.ui.theme.UncategorizedDark
import com.snaxlog.app.ui.theme.UncategorizedLight

/**
 * C-020: MealCategoryHeader
 * FIP-005: Meal Category Classification
 *
 * Sticky header for meal category group in food log.
 * Displays category name, entry count, and nutritional subtotals.
 *
 * @param category Meal category or null for uncategorized.
 * @param entryCount Number of entries in this category.
 * @param totalCalories Sum of calories in category.
 * @param totalProtein Sum of protein in grams.
 * @param totalFat Sum of fat in grams.
 * @param totalCarbs Sum of carbs in grams.
 * @param modifier Modifier for the component.
 * @param isCollapsed Whether category group is collapsed (optional feature).
 * @param onToggleCollapse Callback when header tapped to toggle collapse.
 */
@Composable
fun MealCategoryHeader(
    category: MealCategory?,
    entryCount: Int,
    totalCalories: Int,
    totalProtein: Double,
    totalFat: Double,
    totalCarbs: Double,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val config = remember(category, isDarkTheme) { getCategoryHeaderConfig(category, isDarkTheme) }

    val contentDescriptionText = buildContentDescription(
        config.label,
        entryCount,
        totalCalories,
        totalProtein,
        totalFat,
        totalCarbs
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onToggleCollapse != null) {
                    Modifier.clickable { onToggleCollapse() }
                } else {
                    Modifier
                }
            )
            .semantics {
                role = Role.Tab
                contentDescription = contentDescriptionText
                if (onToggleCollapse != null) {
                    stateDescription = if (isCollapsed) "Collapsed" else "Expanded"
                }
            },
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = Spacing.listItemPaddingHorizontal, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left section: Icon + Label + Count
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = config.color,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = config.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = formatEntryCount(entryCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right section: Nutrition Summary
            Text(
                text = formatNutritionSummary(totalCalories, totalProtein, totalFat, totalCarbs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Configuration for a category header.
 */
private data class CategoryHeaderConfig(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Returns the configuration for a category header.
 */
private fun getCategoryHeaderConfig(category: MealCategory?, isDarkTheme: Boolean): CategoryHeaderConfig {
    return when (category) {
        MealCategory.BREAKFAST -> CategoryHeaderConfig(
            label = "Breakfast",
            icon = Icons.Outlined.WbSunny,
            color = if (isDarkTheme) BreakfastDark else BreakfastLight
        )
        MealCategory.LUNCH -> CategoryHeaderConfig(
            label = "Lunch",
            icon = Icons.Outlined.LightMode,
            color = if (isDarkTheme) LunchDark else LunchLight
        )
        MealCategory.DINNER -> CategoryHeaderConfig(
            label = "Dinner",
            icon = Icons.Outlined.DarkMode,
            color = if (isDarkTheme) DinnerDark else DinnerLight
        )
        MealCategory.SNACKING -> CategoryHeaderConfig(
            label = "Snacking",
            icon = Icons.Outlined.NightsStay,
            color = if (isDarkTheme) SnackingDark else SnackingLight
        )
        null -> CategoryHeaderConfig(
            label = "Uncategorized",
            icon = Icons.Outlined.MoreHoriz,
            color = if (isDarkTheme) UncategorizedDark else UncategorizedLight
        )
    }
}

/**
 * Formats the entry count with proper singular/plural.
 */
private fun formatEntryCount(count: Int): String {
    return "($count item${if (count != 1) "s" else ""})"
}

/**
 * Formats the nutrition summary string.
 */
private fun formatNutritionSummary(
    calories: Int,
    protein: Double,
    fat: Double,
    carbs: Double
): String {
    val proteinInt = protein.toInt()
    val fatInt = fat.toInt()
    val carbsInt = carbs.toInt()
    return "$calories cal | P: ${proteinInt}g F: ${fatInt}g C: ${carbsInt}g"
}

/**
 * Builds the accessibility content description for the header.
 */
private fun buildContentDescription(
    categoryLabel: String,
    entryCount: Int,
    totalCalories: Int,
    totalProtein: Double,
    totalFat: Double,
    totalCarbs: Double
): String {
    val items = if (entryCount == 1) "item" else "items"
    return "$categoryLabel category, $entryCount $items, $totalCalories calories, " +
            "protein ${totalProtein.toInt()} grams, fat ${totalFat.toInt()} grams, carbs ${totalCarbs.toInt()} grams"
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
