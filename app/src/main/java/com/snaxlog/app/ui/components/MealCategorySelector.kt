package com.snaxlog.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
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
import com.snaxlog.app.ui.theme.Spacing
import com.snaxlog.app.ui.theme.UncategorizedDark
import com.snaxlog.app.ui.theme.UncategorizedLight
import kotlinx.coroutines.delay

/**
 * C-019: MealCategorySelector
 * FIP-005: Meal Category Classification
 *
 * Horizontal chip group for selecting meal category during food entry.
 * Shows 5 chips: Breakfast, Lunch, Dinner, Snacking, and None.
 * Supports auto-selection animation when category is suggested based on time.
 *
 * @param selectedCategory Currently selected category, or null if uncategorized.
 * @param onCategoryChange Callback when category selection changes.
 * @param modifier Modifier for the component.
 * @param autoSelectedCategory Category that was auto-selected (for animation hint).
 * @param enabled Whether the selector is interactive.
 * @param showLabel Whether to show the "Meal Category (Optional)" label.
 */
@Composable
fun MealCategorySelector(
    selectedCategory: MealCategory?,
    onCategoryChange: (MealCategory?) -> Unit,
    modifier: Modifier = Modifier,
    autoSelectedCategory: MealCategory? = null,
    enabled: Boolean = true,
    showLabel: Boolean = true
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val chipConfigs = remember { getCategoryChipConfigs() }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabel) {
            Text(
                text = "Meal Category (Optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics { role = Role.RadioButton },
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            chipConfigs.forEach { config ->
                val isSelected = selectedCategory == config.category
                val showAutoAnimation = autoSelectedCategory == config.category &&
                        selectedCategory == config.category

                CategoryFilterChip(
                    config = config,
                    selected = isSelected,
                    onSelect = {
                        if (isSelected) {
                            // Tap on selected chip deselects it
                            onCategoryChange(null)
                        } else {
                            onCategoryChange(config.category)
                        }
                    },
                    showAutoAnimation = showAutoAnimation,
                    enabled = enabled,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

/**
 * Individual category filter chip with selection state and animation support.
 */
@Composable
private fun CategoryFilterChip(
    config: CategoryChipConfig,
    selected: Boolean,
    onSelect: () -> Unit,
    showAutoAnimation: Boolean,
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    var animationTriggered by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }

    // Auto-selection animation (subtle scale pulse)
    LaunchedEffect(showAutoAnimation) {
        if (showAutoAnimation && !animationTriggered) {
            animationTriggered = true
            // Scale up
            scale = 1.05f
            delay(150)
            // Scale down
            scale = 1f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(
            durationMillis = 150,
            easing = FastOutSlowInEasing
        ),
        label = "chip_scale"
    )

    val categoryColor = config.getColor(isDarkTheme)
    val selectionDescription = if (selected) "selected" else "unselected"
    val chipDescription = "${config.label} category chip, $selectionDescription"

    FilterChip(
        selected = selected,
        onClick = onSelect,
        label = { Text(config.label) },
        leadingIcon = {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        enabled = enabled,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = categoryColor.copy(alpha = 0.2f),
            selectedLabelColor = categoryColor,
            selectedLeadingIconColor = categoryColor,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            disabledSelectedContainerColor = categoryColor.copy(alpha = 0.12f)
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderWidth = if (selected) 2.dp else 1.dp,
            borderColor = if (selected) categoryColor else MaterialTheme.colorScheme.outline,
            selectedBorderWidth = 2.dp,
            selectedBorderColor = categoryColor,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f),
            disabledSelectedBorderColor = categoryColor.copy(alpha = 0.38f)
        ),
        modifier = Modifier
            .scale(animatedScale)
            .semantics {
                contentDescription = chipDescription
            }
    )
}

/**
 * Configuration for a category chip.
 */
private data class CategoryChipConfig(
    val category: MealCategory?,
    val label: String,
    val icon: ImageVector,
    val lightColor: Color,
    val darkColor: Color
) {
    fun getColor(isDarkTheme: Boolean): Color =
        if (isDarkTheme) darkColor else lightColor
}

/**
 * Returns the list of category chip configurations.
 * Order: Breakfast, Lunch, Dinner, Snacking, None
 */
private fun getCategoryChipConfigs(): List<CategoryChipConfig> = listOf(
    CategoryChipConfig(
        category = MealCategory.BREAKFAST,
        label = "Breakfast",
        icon = Icons.Outlined.WbSunny,
        lightColor = BreakfastLight,
        darkColor = BreakfastDark
    ),
    CategoryChipConfig(
        category = MealCategory.LUNCH,
        label = "Lunch",
        icon = Icons.Outlined.LightMode,
        lightColor = LunchLight,
        darkColor = LunchDark
    ),
    CategoryChipConfig(
        category = MealCategory.DINNER,
        label = "Dinner",
        icon = Icons.Outlined.DarkMode,
        lightColor = DinnerLight,
        darkColor = DinnerDark
    ),
    CategoryChipConfig(
        category = MealCategory.SNACKING,
        label = "Snacking",
        icon = Icons.Outlined.NightsStay,
        lightColor = SnackingLight,
        darkColor = SnackingDark
    ),
    CategoryChipConfig(
        category = null,
        label = "None",
        icon = Icons.Outlined.RemoveCircleOutline,
        lightColor = UncategorizedLight,
        darkColor = UncategorizedDark
    )
)

/**
 * Extension function to calculate luminance of a color.
 * Used to determine if we're in dark or light theme.
 */
private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
