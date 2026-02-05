package com.snaxlog.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.NumberFormat

/**
 * Tests for DailySummaryCard logic functions.
 * Tests the pure logic (remaining text and color computation) without Compose dependency.
 *
 * Covers:
 * - Remaining text formatting for various progress states
 * - Color assignment based on progress thresholds
 * - Edge cases: null remaining, exactly at goal, over goal, approaching limit
 */
class DailySummaryCardLogicTest {

    private val numberFormat = NumberFormat.getNumberInstance()
    private val successColor = Color(0xFF2E7D32)
    private val warningColor = Color(0xFFF57C00)
    private val errorColor = Color(0xFFD32F2F)
    private val defaultColor = Color(0xFF1B5E20)

    // ============================================================
    // getRemainingTextAndColor - null remaining
    // ============================================================

    @Test
    fun `null remaining returns empty text with default color`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = null,
            progress = 0f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("", text)
        assertEquals(defaultColor, color)
    }

    // ============================================================
    // getRemainingTextAndColor - Normal state (0-89%)
    // ============================================================

    @Test
    fun `normal remaining shows remaining text with default color`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = 500,
            progress = 0.75f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("${numberFormat.format(500)} remaining", text)
        assertEquals(defaultColor, color)
    }

    @Test
    fun `large remaining value formats correctly`() {
        val (text, _) = getRemainingTextAndColor(
            remaining = 1500,
            progress = 0.25f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("${numberFormat.format(1500)} remaining", text)
    }

    // ============================================================
    // getRemainingTextAndColor - Approaching state (90-100%)
    // ============================================================

    @Test
    fun `approaching limit shows warning text at 90 percent`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = 200,
            progress = 0.9f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("${numberFormat.format(200)} remaining - approaching limit", text)
        assertEquals(warningColor, color)
    }

    @Test
    fun `approaching limit shows warning text at 95 percent`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = 100,
            progress = 0.95f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("${numberFormat.format(100)} remaining - approaching limit", text)
        assertEquals(warningColor, color)
    }

    // ============================================================
    // getRemainingTextAndColor - Goal reached (exactly 0 remaining)
    // ============================================================

    @Test
    fun `EC-087 - exactly at goal shows goal reached with success color`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = 0,
            progress = 1.0f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("Goal reached!", text)
        assertEquals(successColor, color)
    }

    // ============================================================
    // getRemainingTextAndColor - Exceeded state (over goal)
    // ============================================================

    @Test
    fun `AC-055 - over goal shows error text with amount over`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = -200,
            progress = 1.1f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("Over goal by ${numberFormat.format(200)}", text)
        assertEquals(errorColor, color)
    }

    @Test
    fun `greatly over goal shows correct amount`() {
        val (text, color) = getRemainingTextAndColor(
            remaining = -1000,
            progress = 1.5f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("Over goal by ${numberFormat.format(1000)}", text)
        assertEquals(errorColor, color)
    }

    // ============================================================
    // Priority ordering: remaining == 0 takes priority over progress >= 0.9
    // ============================================================

    @Test
    fun `remaining 0 with approaching progress shows goal reached not approaching`() {
        // When remaining is exactly 0, "Goal reached!" is shown regardless of progress threshold
        val (text, _) = getRemainingTextAndColor(
            remaining = 0,
            progress = 0.999f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("Goal reached!", text)
    }

    @Test
    fun `negative remaining takes priority over approaching text`() {
        // When over goal, "Over goal by X" is shown even at 90% progress edge case
        val (text, _) = getRemainingTextAndColor(
            remaining = -1,
            progress = 0.95f,
            numberFormat = numberFormat,
            successColor = successColor,
            warningColor = warningColor,
            errorColor = errorColor,
            defaultColor = defaultColor
        )

        assertEquals("Over goal by ${numberFormat.format(1)}", text)
    }
}
