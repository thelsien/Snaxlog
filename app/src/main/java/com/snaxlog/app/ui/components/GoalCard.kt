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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.Spacing
import java.text.NumberFormat

/**
 * C-012: GoalCard
 * Displays a calorie goal with active indicator.
 * Supports swipe-to-delete for custom goals.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(
    goalName: String,
    calorieTarget: Int,
    isActive: Boolean,
    isPredefined: Boolean,
    proteinTarget: Double? = null,
    fatTarget: Double? = null,
    carbsTarget: Double? = null,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance()
    val typeLabel = if (isPredefined) "Pre-defined" else "Custom"
    val activeLabel = if (isActive) "Active goal." else ""
    val macroInfo = buildMacroDescription(proteinTarget, fatTarget, carbsTarget)
    val description = "$goalName, ${numberFormat.format(calorieTarget)} calories. $typeLabel. $activeLabel $macroInfo Tap to select."

    val cardShape = RoundedCornerShape(12.dp)
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    // Swipe-to-delete only for custom goals
    if (!isPredefined && onDelete != null) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    false
                } else {
                    false
                }
            }
        )

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
                            shape = cardShape
                        )
                        .padding(horizontal = Spacing.base),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete goal",
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            enableDismissFromStartToEnd = false
        ) {
            GoalCardContent(
                goalName = goalName,
                calorieTarget = calorieTarget,
                isActive = isActive,
                isPredefined = isPredefined,
                proteinTarget = proteinTarget,
                fatTarget = fatTarget,
                carbsTarget = carbsTarget,
                onClick = onClick,
                onEdit = onEdit,
                containerColor = containerColor,
                description = description
            )
        }
    } else {
        // Non-swipeable version for predefined goals
        Box(
            modifier = modifier.padding(
                horizontal = Spacing.cardMarginHorizontal,
                vertical = Spacing.cardMarginVertical
            )
        ) {
            GoalCardContent(
                goalName = goalName,
                calorieTarget = calorieTarget,
                isActive = isActive,
                isPredefined = isPredefined,
                proteinTarget = proteinTarget,
                fatTarget = fatTarget,
                carbsTarget = carbsTarget,
                onClick = onClick,
                onEdit = null,
                containerColor = containerColor,
                description = description
            )
        }
    }
}

@Composable
private fun GoalCardContent(
    goalName: String,
    calorieTarget: Int,
    isActive: Boolean,
    isPredefined: Boolean,
    proteinTarget: Double?,
    fatTarget: Double?,
    carbsTarget: Double?,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    containerColor: androidx.compose.ui.graphics.Color,
    description: String
) {
    val numberFormat = NumberFormat.getNumberInstance()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.cardPadding, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Goal info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = goalName,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!isPredefined) {
                        Text(
                            text = "Custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${numberFormat.format(calorieTarget)} cal",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isActive) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.padding(top = Spacing.xs)
                )

                // Show macro targets if available
                if (proteinTarget != null || fatTarget != null || carbsTarget != null) {
                    val macroText = buildMacroText(proteinTarget, fatTarget, carbsTarget)
                    Text(
                        text = macroText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = Spacing.xxs)
                    )
                }
            }

            // Edit button for custom goals
            if (!isPredefined && onEdit != null) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit goal",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Active goal",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun buildMacroText(protein: Double?, fat: Double?, carbs: Double?): String {
    val parts = mutableListOf<String>()
    protein?.let { parts.add("P: ${formatMacroValue(it)}g") }
    fat?.let { parts.add("F: ${formatMacroValue(it)}g") }
    carbs?.let { parts.add("C: ${formatMacroValue(it)}g") }
    return parts.joinToString("  ")
}

private fun buildMacroDescription(protein: Double?, fat: Double?, carbs: Double?): String {
    val parts = mutableListOf<String>()
    protein?.let { parts.add("Protein: ${formatMacroValue(it)}g") }
    fat?.let { parts.add("Fat: ${formatMacroValue(it)}g") }
    carbs?.let { parts.add("Carbs: ${formatMacroValue(it)}g") }
    return if (parts.isNotEmpty()) parts.joinToString(", ") + "." else ""
}

private fun formatMacroValue(value: Double): String {
    return if (value == value.toLong().toDouble()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}
