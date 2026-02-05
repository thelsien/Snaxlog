package com.snaxlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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

/**
 * C-021: CategoryBadge
 * FIP-005: Meal Category Classification
 *
 * Small badge showing meal category on food entry card.
 * Shows category icon in a colored circular background.
 *
 * @param category Meal category to display.
 * @param modifier Modifier for the component.
 */
@Composable
fun CategoryBadge(
    category: MealCategory,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val config = getCategoryBadgeConfig(category, isDarkTheme)

    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = config.color.copy(alpha = 0.12f),
                shape = CircleShape
            )
            .padding(4.dp)
            .semantics {
                contentDescription = "${config.label} category"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = null,
            tint = config.color,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Configuration for a category badge.
 */
private data class CategoryBadgeConfig(
    val label: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Returns the configuration for a category badge.
 */
private fun getCategoryBadgeConfig(category: MealCategory, isDarkTheme: Boolean): CategoryBadgeConfig {
    return when (category) {
        MealCategory.BREAKFAST -> CategoryBadgeConfig(
            label = "Breakfast",
            icon = Icons.Outlined.WbSunny,
            color = if (isDarkTheme) BreakfastDark else BreakfastLight
        )
        MealCategory.LUNCH -> CategoryBadgeConfig(
            label = "Lunch",
            icon = Icons.Outlined.LightMode,
            color = if (isDarkTheme) LunchDark else LunchLight
        )
        MealCategory.DINNER -> CategoryBadgeConfig(
            label = "Dinner",
            icon = Icons.Outlined.DarkMode,
            color = if (isDarkTheme) DinnerDark else DinnerLight
        )
        MealCategory.SNACKING -> CategoryBadgeConfig(
            label = "Snacking",
            icon = Icons.Outlined.NightsStay,
            color = if (isDarkTheme) SnackingDark else SnackingLight
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
