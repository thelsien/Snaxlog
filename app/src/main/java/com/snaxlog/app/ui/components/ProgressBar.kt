package com.snaxlog.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snaxlog.app.ui.theme.SnaxlogThemeExtras

/**
 * The threshold at which progress bar changes from success (green) to warning (orange).
 */
const val PROGRESS_THRESHOLD_WARNING = 0.9f

/**
 * The threshold at which progress bar changes from warning (orange) to error (red).
 */
const val PROGRESS_THRESHOLD_EXCEEDED = 1.0f

/**
 * Maximum value for the visual progress indicator display.
 * Actual numbers are still shown beyond this, but the bar caps here.
 * Per EC-089: Progress indicator caps at reasonable max (150%).
 */
const val PROGRESS_VISUAL_CAP = 1.5f

/**
 * C-004: ProgressBar
 *
 * Animated horizontal progress bar for calorie/macro goal tracking.
 * Color changes based on progress percentage:
 * - 0-89%: Success green (within goal)
 * - 90-100%: Warning orange (approaching goal)
 * - 100%+: Error red (exceeded goal)
 *
 * Per design spec: Height 8dp, corner radius 8dp, track color surfaceVariant.
 * Animation: Smooth 300ms with standard easing.
 *
 * @param progress Value from 0.0 to any positive float. Visually capped at [PROGRESS_VISUAL_CAP].
 * @param modifier Optional modifier for layout customization.
 */
@Composable
fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val customColors = SnaxlogThemeExtras.customColors

    val progressColor = getProgressColor(
        progress = progress,
        successColor = customColors.success,
        warningColor = customColors.warning,
        errorColor = MaterialTheme.colorScheme.error
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, PROGRESS_VISUAL_CAP),
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )

    val animatedColor by animateColorAsState(
        targetValue = progressColor,
        animationSpec = tween(durationMillis = 300),
        label = "progress_color_animation"
    )

    val percentage = (progress * 100).toInt().coerceAtLeast(0)
    val accessibilityText = "$percentage% of goal"

    LinearProgressIndicator(
        progress = { animatedProgress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = accessibilityText },
        color = animatedColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

/**
 * Determines the appropriate color for the progress bar based on the progress value.
 *
 * @param progress The current progress as a float (0.0 = 0%, 1.0 = 100%).
 * @param successColor Color for 0-89% progress.
 * @param warningColor Color for 90-100% progress.
 * @param errorColor Color for 100%+ progress.
 * @return The appropriate [Color] for the given progress.
 */
fun getProgressColor(
    progress: Float,
    successColor: Color,
    warningColor: Color,
    errorColor: Color
): Color {
    return when {
        progress < PROGRESS_THRESHOLD_WARNING -> successColor
        progress <= PROGRESS_THRESHOLD_EXCEEDED -> warningColor
        else -> errorColor
    }
}
