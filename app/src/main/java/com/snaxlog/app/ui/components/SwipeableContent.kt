package com.snaxlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * C-026: SwipeableContent
 *
 * A wrapper component that adds horizontal swipe gesture detection for date navigation.
 * Part of FIP-EPIC-005: Historical Day Viewing (US-013).
 *
 * Features:
 * - Detects horizontal swipe gestures (left = next day, right = previous day)
 * - Provides visual feedback during swipe (parallax offset + fade)
 * - Handles boundary behavior (prevents swipe forward from today)
 * - Configurable swipe threshold for triggering navigation
 * - Note: Does NOT conflict with swipe-to-delete as vertical vs horizontal
 *
 * Swipe behavior:
 * - Swipe right (finger moves right): Navigate to previous day (always allowed)
 * - Swipe left (finger moves left): Navigate to next day (blocked when on today)
 *
 * Visual feedback:
 * - Content offsets in the swipe direction (max 50% screen width)
 * - Content fades slightly during swipe (min 80% opacity)
 * - Smooth spring animation when released
 *
 * @param onSwipeLeft Callback when user swipes left (typically "next day")
 * @param onSwipeRight Callback when user swipes right (typically "previous day")
 * @param canSwipeLeft Whether left swipe is enabled (false when viewing today)
 * @param canSwipeRight Whether right swipe is enabled (typically always true)
 * @param swipeThreshold The minimum swipe distance (in dp) to trigger navigation
 * @param modifier Compose modifier for customization
 * @param content The composable content to wrap
 */
@Composable
fun SwipeableContent(
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    canSwipeLeft: Boolean = true,
    canSwipeRight: Boolean = true,
    swipeThreshold: Float = 100f,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Maximum offset is 50% of screen width
    val maxOffset = screenWidthPx * 0.5f

    // Current drag offset
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Animated offset for smooth return
    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "swipe_offset_animation"
    )

    // Fade effect based on offset (80% to 100% opacity)
    val alpha = (1f - (abs(animatedOffset) / maxOffset) * 0.2f).coerceIn(0.8f, 1f)

    Box(
        modifier = modifier
            .pointerInput(canSwipeLeft, canSwipeRight) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        // Check if swipe threshold was exceeded
                        val swipeDistance = abs(offsetX)
                        val swipeThresholdPx = swipeThreshold * density.density

                        if (swipeDistance >= swipeThresholdPx) {
                            if (offsetX > 0 && canSwipeRight) {
                                // Swiped right -> previous day
                                onSwipeRight()
                            } else if (offsetX < 0 && canSwipeLeft) {
                                // Swiped left -> next day
                                onSwipeLeft()
                            }
                        }

                        // Reset offset
                        offsetX = 0f
                        isDragging = false
                    },
                    onDragCancel = {
                        offsetX = 0f
                        isDragging = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()

                        // Calculate new offset, clamped to max
                        val newOffset = offsetX + dragAmount

                        // Only allow drag in enabled directions
                        val clampedOffset = when {
                            newOffset > 0 && !canSwipeRight -> 0f
                            newOffset < 0 && !canSwipeLeft -> 0f
                            else -> newOffset.coerceIn(-maxOffset, maxOffset)
                        }

                        offsetX = clampedOffset
                    }
                )
            }
            .offset { IntOffset(animatedOffset.roundToInt(), 0) }
            .alpha(alpha)
    ) {
        content()
    }
}

/**
 * A simpler version of SwipeableContent specifically for date navigation.
 * Handles the common case of navigating between days.
 *
 * @param selectedDate Not used directly, but useful for recomposition
 * @param onNavigateToPrevious Callback to navigate to previous day
 * @param onNavigateToNext Callback to navigate to next day
 * @param canNavigateToNext Whether forward navigation is allowed
 * @param modifier Compose modifier for customization
 * @param content The composable content to wrap
 */
@Composable
fun DateSwipeContainer(
    onNavigateToPrevious: () -> Unit,
    onNavigateToNext: () -> Unit,
    canNavigateToNext: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    SwipeableContent(
        onSwipeLeft = onNavigateToNext,  // Swipe left = go forward (next day)
        onSwipeRight = onNavigateToPrevious,  // Swipe right = go back (previous day)
        canSwipeLeft = canNavigateToNext,  // Can only swipe forward if not on today
        canSwipeRight = true,  // Can always swipe back (EC-094)
        swipeThreshold = 80f,  // Slightly lower threshold for better UX
        modifier = modifier,
        content = content
    )
}
