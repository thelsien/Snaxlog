package com.snaxlog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.text.NumberFormat

/**
 * C-002: FoodEntryCard
 * Displays a single food intake entry with swipe-to-delete gesture.
 * FIP-005: Added meal category badge and colored accent border.
 *
 * @param mealCategory Optional meal category for visual badge (FIP-005).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEntryCard(
    foodName: String,
    servingSize: String,
    calories: Int,
    timestamp: String,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    mealCategory: MealCategory? = null
) {
    val numberFormat = NumberFormat.getNumberInstance()
    val description = "$foodName, $servingSize, ${numberFormat.format(calories)} calories, logged at $timestamp. Tap to edit, swipe to delete."

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false // Don't actually dismiss, let the dialog handle it
            } else {
                false
            }
        }
    )

    // Reset swipe state after dismiss action
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(
            horizontal = Spacing.cardMarginHorizontal,
            vertical = Spacing.cardMarginVertical
        ),
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = Spacing.base),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete entry",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        // FIP-005: Get category color for accent border
        val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val categoryColor = mealCategory?.let { getCategoryColor(it, isDarkTheme) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .semantics { contentDescription = description }
                // FIP-005: Add subtle left border in category color
                .then(
                    if (categoryColor != null) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = 1.dp,
                                color = categoryColor.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(
                        horizontal = Spacing.cardPadding,
                        vertical = Spacing.md
                    )
                ) {
                    // Primary row: Food name + Calories
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = foodName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = numberFormat.format(calories),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = " cal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Serving info
                    Text(
                        text = servingSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs)
                    )

                    // Timestamp
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xxs)
                    )
                }

                // FIP-005: Category badge in top-right corner
                if (mealCategory != null) {
                    CategoryBadge(
                        category = mealCategory,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = Spacing.sm, end = Spacing.sm)
                    )
                }
            }
        }
    }
}

/**
 * FIP-005: Get the color for a meal category.
 */
private fun getCategoryColor(category: MealCategory, isDarkTheme: Boolean): Color {
    return when (category) {
        MealCategory.BREAKFAST -> if (isDarkTheme) BreakfastDark else BreakfastLight
        MealCategory.LUNCH -> if (isDarkTheme) LunchDark else LunchLight
        MealCategory.DINNER -> if (isDarkTheme) DinnerDark else DinnerLight
        MealCategory.SNACKING -> if (isDarkTheme) SnackingDark else SnackingLight
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
