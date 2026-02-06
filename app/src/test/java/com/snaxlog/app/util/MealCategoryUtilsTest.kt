package com.snaxlog.app.util

import com.snaxlog.app.data.local.entity.MealCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

/**
 * Unit tests for MealCategoryUtils.
 * FIP-005: Meal Category Classification
 */
class MealCategoryUtilsTest {

    // ========================================
    // getCurrentMealCategory tests
    // ========================================

    @Test
    fun `breakfast at 4 00 AM returns BREAKFAST`() {
        val time = LocalTime.of(4, 0)
        assertEquals(MealCategory.BREAKFAST, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `breakfast at 7 30 AM returns BREAKFAST`() {
        val time = LocalTime.of(7, 30)
        assertEquals(MealCategory.BREAKFAST, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `breakfast at 10 59 AM returns BREAKFAST`() {
        val time = LocalTime.of(10, 59)
        assertEquals(MealCategory.BREAKFAST, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `lunch at 11 00 AM returns LUNCH`() {
        // Boundary test: 11:00 AM is the start of lunch
        val time = LocalTime.of(11, 0)
        assertEquals(MealCategory.LUNCH, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `lunch at 12 30 PM returns LUNCH`() {
        val time = LocalTime.of(12, 30)
        assertEquals(MealCategory.LUNCH, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `lunch at 14 59 returns LUNCH`() {
        val time = LocalTime.of(14, 59)
        assertEquals(MealCategory.LUNCH, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `dinner at 15 00 returns DINNER`() {
        // Boundary test: 15:00 (3 PM) is the start of dinner
        val time = LocalTime.of(15, 0)
        assertEquals(MealCategory.DINNER, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `dinner at 18 00 returns DINNER`() {
        val time = LocalTime.of(18, 0)
        assertEquals(MealCategory.DINNER, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `dinner at 20 59 returns DINNER`() {
        val time = LocalTime.of(20, 59)
        assertEquals(MealCategory.DINNER, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `snacking at 21 00 returns SNACKING`() {
        // Boundary test: 21:00 (9 PM) is the start of snacking
        val time = LocalTime.of(21, 0)
        assertEquals(MealCategory.SNACKING, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `snacking at 23 30 returns SNACKING`() {
        val time = LocalTime.of(23, 30)
        assertEquals(MealCategory.SNACKING, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `snacking at midnight returns SNACKING`() {
        val time = LocalTime.of(0, 0)
        assertEquals(MealCategory.SNACKING, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `snacking at 2 00 AM returns SNACKING`() {
        val time = LocalTime.of(2, 0)
        assertEquals(MealCategory.SNACKING, MealCategoryUtils.getCurrentMealCategory(time))
    }

    @Test
    fun `snacking at 3 59 AM returns SNACKING`() {
        val time = LocalTime.of(3, 59)
        assertEquals(MealCategory.SNACKING, MealCategoryUtils.getCurrentMealCategory(time))
    }

    // ========================================
    // getCategoryDisplayName tests
    // ========================================

    @Test
    fun `display name for BREAKFAST is Breakfast`() {
        assertEquals("Breakfast", MealCategoryUtils.getCategoryDisplayName(MealCategory.BREAKFAST))
    }

    @Test
    fun `display name for LUNCH is Lunch`() {
        assertEquals("Lunch", MealCategoryUtils.getCategoryDisplayName(MealCategory.LUNCH))
    }

    @Test
    fun `display name for DINNER is Dinner`() {
        assertEquals("Dinner", MealCategoryUtils.getCategoryDisplayName(MealCategory.DINNER))
    }

    @Test
    fun `display name for SNACKING is Snacking`() {
        assertEquals("Snacking", MealCategoryUtils.getCategoryDisplayName(MealCategory.SNACKING))
    }

    @Test
    fun `display name for null is Uncategorized`() {
        assertEquals("Uncategorized", MealCategoryUtils.getCategoryDisplayName(null))
    }

    // ========================================
    // getCategorySortOrder tests
    // ========================================

    @Test
    fun `sort order for null (uncategorized) is 0`() {
        assertEquals(0, MealCategoryUtils.getCategorySortOrder(null))
    }

    @Test
    fun `sort order for BREAKFAST is 1`() {
        assertEquals(1, MealCategoryUtils.getCategorySortOrder(MealCategory.BREAKFAST))
    }

    @Test
    fun `sort order for LUNCH is 2`() {
        assertEquals(2, MealCategoryUtils.getCategorySortOrder(MealCategory.LUNCH))
    }

    @Test
    fun `sort order for DINNER is 3`() {
        assertEquals(3, MealCategoryUtils.getCategorySortOrder(MealCategory.DINNER))
    }

    @Test
    fun `sort order for SNACKING is 4`() {
        assertEquals(4, MealCategoryUtils.getCategorySortOrder(MealCategory.SNACKING))
    }

    @Test
    fun `categories sort in correct order`() {
        val categories = listOf(
            MealCategory.SNACKING,
            MealCategory.BREAKFAST,
            null,
            MealCategory.DINNER,
            MealCategory.LUNCH
        )

        val sorted = categories.sortedBy { MealCategoryUtils.getCategorySortOrder(it) }

        assertEquals(
            listOf(null, MealCategory.BREAKFAST, MealCategory.LUNCH, MealCategory.DINNER, MealCategory.SNACKING),
            sorted
        )
    }

    // ========================================
    // getCategoryTimeRange tests
    // ========================================

    @Test
    fun `time range for BREAKFAST is correct`() {
        assertEquals("4:00 AM - 11:00 AM", MealCategoryUtils.getCategoryTimeRange(MealCategory.BREAKFAST))
    }

    @Test
    fun `time range for LUNCH is correct`() {
        assertEquals("11:00 AM - 3:00 PM", MealCategoryUtils.getCategoryTimeRange(MealCategory.LUNCH))
    }

    @Test
    fun `time range for DINNER is correct`() {
        assertEquals("3:00 PM - 9:00 PM", MealCategoryUtils.getCategoryTimeRange(MealCategory.DINNER))
    }

    @Test
    fun `time range for SNACKING is correct`() {
        assertEquals("9:00 PM - 4:00 AM", MealCategoryUtils.getCategoryTimeRange(MealCategory.SNACKING))
    }

    @Test
    fun `time range for null is empty`() {
        assertEquals("", MealCategoryUtils.getCategoryTimeRange(null))
    }
}
