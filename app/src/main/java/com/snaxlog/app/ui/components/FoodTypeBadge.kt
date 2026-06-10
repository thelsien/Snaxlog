package com.snaxlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.R
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.ui.theme.CustomFoodDark
import com.snaxlog.app.ui.theme.CustomFoodLight
import com.snaxlog.app.ui.theme.RecipeDark
import com.snaxlog.app.ui.theme.RecipeLight
import com.snaxlog.app.ui.theme.Spacing

/**
 * C-027: FoodTypeBadge
 * EPIC-006: User-Created Foods and Recipes
 * US-020 AC-015-001: Custom foods appear in search with badges
 *
 * Visual badge indicating whether a food is a custom food or a recipe.
 * Only shown for user-created foods (SIMPLE or RECIPE types).
 *
 * @param foodType The type of food (SIMPLE shows "Custom", RECIPE shows "Recipe").
 * @param modifier Modifier for the component.
 * @param showLabel Whether to show the text label alongside the icon.
 */
@Composable
fun FoodTypeBadge(
    foodType: FoodType,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    // Only show badge for user-created foods
    if (foodType == FoodType.PREDEFINED) return

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val config = getFoodTypeBadgeConfig(foodType, isDarkTheme)

    val label = stringResource(config.labelRes)
    val badgeDescription = stringResource(R.string.food_type_badge_description, label)

    Row(
        modifier = modifier
            .background(
                color = config.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = Spacing.xs, vertical = Spacing.xxs)
            .semantics {
                contentDescription = badgeDescription
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.color,
            modifier = Modifier.size(12.dp)
        )
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = config.color,
                modifier = Modifier.padding(start = Spacing.xxs)
            )
        }
    }
}

/**
 * Configuration for a food type badge.
 */
private data class FoodTypeBadgeConfig(
    val labelRes: Int,
    val icon: ImageVector,
    val color: Color
)

/**
 * Returns the configuration for a food type badge.
 */
private fun getFoodTypeBadgeConfig(foodType: FoodType, isDarkTheme: Boolean): FoodTypeBadgeConfig {
    return when (foodType) {
        FoodType.SIMPLE -> FoodTypeBadgeConfig(
            labelRes = R.string.food_type_custom,
            icon = Icons.Outlined.Person,
            color = if (isDarkTheme) CustomFoodDark else CustomFoodLight
        )
        FoodType.RECIPE -> FoodTypeBadgeConfig(
            labelRes = R.string.food_type_recipe,
            icon = Icons.Outlined.Restaurant,
            color = if (isDarkTheme) RecipeDark else RecipeLight
        )
        FoodType.PREDEFINED -> FoodTypeBadgeConfig(
            labelRes = R.string.food_type_custom,
            icon = Icons.Outlined.Person,
            color = Color.Transparent
        )
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
