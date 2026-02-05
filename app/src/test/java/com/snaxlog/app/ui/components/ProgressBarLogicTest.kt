package com.snaxlog.app.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the ProgressBar color logic and constants (C-004).
 * Tests the pure logic functions without Compose UI dependency.
 *
 * Covers:
 * - Color threshold boundaries: 0-89% (success), 90-100% (warning), 100%+ (error)
 * - Constants for thresholds and visual cap
 */
class ProgressBarLogicTest {

    private val successColor = Color(0xFF2E7D32)  // Green
    private val warningColor = Color(0xFFF57C00)  // Orange
    private val errorColor = Color(0xFFD32F2F)    // Red

    // ============================================================
    // Constants
    // ============================================================

    @Test
    fun `warning threshold is 0_9`() {
        assertEquals(0.9f, PROGRESS_THRESHOLD_WARNING, 0.001f)
    }

    @Test
    fun `exceeded threshold is 1_0`() {
        assertEquals(1.0f, PROGRESS_THRESHOLD_EXCEEDED, 0.001f)
    }

    @Test
    fun `visual cap is 1_5`() {
        assertEquals(1.5f, PROGRESS_VISUAL_CAP, 0.001f)
    }

    // ============================================================
    // getProgressColor - Success range (0-89%)
    // ============================================================

    @Test
    fun `getProgressColor - 0 percent returns success color`() {
        val result = getProgressColor(0.0f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }

    @Test
    fun `getProgressColor - 50 percent returns success color`() {
        val result = getProgressColor(0.5f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }

    @Test
    fun `getProgressColor - 89 percent returns success color`() {
        val result = getProgressColor(0.89f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }

    @Test
    fun `getProgressColor - 89_9 percent returns success color`() {
        val result = getProgressColor(0.899f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }

    // ============================================================
    // getProgressColor - Warning range (90-100%)
    // ============================================================

    @Test
    fun `getProgressColor - 90 percent returns warning color`() {
        val result = getProgressColor(0.9f, successColor, warningColor, errorColor)
        assertEquals(warningColor, result)
    }

    @Test
    fun `getProgressColor - 95 percent returns warning color`() {
        val result = getProgressColor(0.95f, successColor, warningColor, errorColor)
        assertEquals(warningColor, result)
    }

    @Test
    fun `getProgressColor - 100 percent returns warning color`() {
        val result = getProgressColor(1.0f, successColor, warningColor, errorColor)
        assertEquals(warningColor, result)
    }

    // ============================================================
    // getProgressColor - Error range (100%+)
    // ============================================================

    @Test
    fun `getProgressColor - 101 percent returns error color`() {
        val result = getProgressColor(1.01f, successColor, warningColor, errorColor)
        assertEquals(errorColor, result)
    }

    @Test
    fun `getProgressColor - 150 percent returns error color`() {
        val result = getProgressColor(1.5f, successColor, warningColor, errorColor)
        assertEquals(errorColor, result)
    }

    @Test
    fun `getProgressColor - 200 percent returns error color`() {
        val result = getProgressColor(2.0f, successColor, warningColor, errorColor)
        assertEquals(errorColor, result)
    }

    @Test
    fun `getProgressColor - 300 percent returns error color`() {
        val result = getProgressColor(3.0f, successColor, warningColor, errorColor)
        assertEquals(errorColor, result)
    }

    // ============================================================
    // getProgressColor - Edge cases
    // ============================================================

    @Test
    fun `getProgressColor - negative progress returns success color`() {
        val result = getProgressColor(-0.1f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }

    @Test
    fun `getProgressColor - very small positive returns success color`() {
        val result = getProgressColor(0.001f, successColor, warningColor, errorColor)
        assertEquals(successColor, result)
    }
}
