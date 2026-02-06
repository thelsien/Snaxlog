package com.snaxlog.app.data.local.database

import com.snaxlog.app.data.local.entity.MealCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for Room TypeConverters.
 * FIP-005: Meal Category Classification
 */
class ConvertersTest {

    private lateinit var converters: Converters

    @Before
    fun setup() {
        converters = Converters()
    }

    // ========================================
    // fromMealCategory tests
    // ========================================

    @Test
    fun `fromMealCategory with BREAKFAST returns BREAKFAST string`() {
        assertEquals("BREAKFAST", converters.fromMealCategory(MealCategory.BREAKFAST))
    }

    @Test
    fun `fromMealCategory with LUNCH returns LUNCH string`() {
        assertEquals("LUNCH", converters.fromMealCategory(MealCategory.LUNCH))
    }

    @Test
    fun `fromMealCategory with DINNER returns DINNER string`() {
        assertEquals("DINNER", converters.fromMealCategory(MealCategory.DINNER))
    }

    @Test
    fun `fromMealCategory with SNACKING returns SNACKING string`() {
        assertEquals("SNACKING", converters.fromMealCategory(MealCategory.SNACKING))
    }

    @Test
    fun `fromMealCategory with null returns null`() {
        assertNull(converters.fromMealCategory(null))
    }

    // ========================================
    // toMealCategory tests
    // ========================================

    @Test
    fun `toMealCategory with BREAKFAST string returns BREAKFAST`() {
        assertEquals(MealCategory.BREAKFAST, converters.toMealCategory("BREAKFAST"))
    }

    @Test
    fun `toMealCategory with LUNCH string returns LUNCH`() {
        assertEquals(MealCategory.LUNCH, converters.toMealCategory("LUNCH"))
    }

    @Test
    fun `toMealCategory with DINNER string returns DINNER`() {
        assertEquals(MealCategory.DINNER, converters.toMealCategory("DINNER"))
    }

    @Test
    fun `toMealCategory with SNACKING string returns SNACKING`() {
        assertEquals(MealCategory.SNACKING, converters.toMealCategory("SNACKING"))
    }

    @Test
    fun `toMealCategory with null returns null`() {
        assertNull(converters.toMealCategory(null))
    }

    @Test
    fun `toMealCategory with invalid string returns null`() {
        assertNull(converters.toMealCategory("INVALID"))
    }

    @Test
    fun `toMealCategory with empty string returns null`() {
        assertNull(converters.toMealCategory(""))
    }

    @Test
    fun `toMealCategory with lowercase string returns null`() {
        // Enum.valueOf is case-sensitive
        assertNull(converters.toMealCategory("breakfast"))
    }

    // ========================================
    // Round-trip tests
    // ========================================

    @Test
    fun `round trip conversion for BREAKFAST`() {
        val original = MealCategory.BREAKFAST
        val converted = converters.fromMealCategory(original)
        val restored = converters.toMealCategory(converted)
        assertEquals(original, restored)
    }

    @Test
    fun `round trip conversion for LUNCH`() {
        val original = MealCategory.LUNCH
        val converted = converters.fromMealCategory(original)
        val restored = converters.toMealCategory(converted)
        assertEquals(original, restored)
    }

    @Test
    fun `round trip conversion for DINNER`() {
        val original = MealCategory.DINNER
        val converted = converters.fromMealCategory(original)
        val restored = converters.toMealCategory(converted)
        assertEquals(original, restored)
    }

    @Test
    fun `round trip conversion for SNACKING`() {
        val original = MealCategory.SNACKING
        val converted = converters.fromMealCategory(original)
        val restored = converters.toMealCategory(converted)
        assertEquals(original, restored)
    }

    @Test
    fun `round trip conversion for null`() {
        val original: MealCategory? = null
        val converted = converters.fromMealCategory(original)
        val restored = converters.toMealCategory(converted)
        assertEquals(original, restored)
    }
}
