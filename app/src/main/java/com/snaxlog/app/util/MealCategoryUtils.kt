package com.snaxlog.app.util

import com.snaxlog.app.data.local.entity.MealCategory
import java.time.LocalTime

/**
 * Utility functions for meal category classification.
 * FIP-005: Meal Category Classification
 */
object MealCategoryUtils {

    /**
     * Time boundaries for meal categories (hour of day).
     * BREAKFAST: 04:00 - 10:59 (hours 4-10)
     * LUNCH: 11:00 - 14:59 (hours 11-14)
     * DINNER: 15:00 - 20:59 (hours 15-20)
     * SNACKING: 21:00 - 03:59 (hours 21-23, 0-3)
     */
    private const val BREAKFAST_START_HOUR = 4
    private const val BREAKFAST_END_HOUR = 10   // 10:59 is still breakfast
    private const val LUNCH_START_HOUR = 11
    private const val LUNCH_END_HOUR = 14       // 14:59 is still lunch
    private const val DINNER_START_HOUR = 15
    private const val DINNER_END_HOUR = 20      // 20:59 is still dinner
    // SNACKING: 21:00-03:59 (remaining hours)

    /**
     * Determines the meal category based on the given time.
     *
     * @param time The time to evaluate. Defaults to current device time.
     * @return The appropriate MealCategory based on time of day.
     *
     * Time ranges:
     * - BREAKFAST: 04:00 - 10:59
     * - LUNCH: 11:00 - 14:59
     * - DINNER: 15:00 - 20:59
     * - SNACKING: 21:00 - 03:59
     */
    fun getCurrentMealCategory(time: LocalTime = LocalTime.now()): MealCategory {
        val hour = time.hour
        return when (hour) {
            in BREAKFAST_START_HOUR..BREAKFAST_END_HOUR -> MealCategory.BREAKFAST
            in LUNCH_START_HOUR..LUNCH_END_HOUR -> MealCategory.LUNCH
            in DINNER_START_HOUR..DINNER_END_HOUR -> MealCategory.DINNER
            else -> MealCategory.SNACKING // 21-23 or 0-3
        }
    }

    /**
     * Returns the display name for a meal category.
     *
     * @param category The meal category, or null for uncategorized.
     * @return The human-readable display name.
     */
    fun getCategoryDisplayName(category: MealCategory?): String {
        return when (category) {
            MealCategory.BREAKFAST -> "Breakfast"
            MealCategory.LUNCH -> "Lunch"
            MealCategory.DINNER -> "Dinner"
            MealCategory.SNACKING -> "Snacking"
            null -> "Uncategorized"
        }
    }

    /**
     * Returns the time range description for a meal category.
     *
     * @param category The meal category.
     * @return The time range string (e.g., "4:00 AM - 11:00 AM").
     */
    fun getCategoryTimeRange(category: MealCategory?): String {
        return when (category) {
            MealCategory.BREAKFAST -> "4:00 AM - 11:00 AM"
            MealCategory.LUNCH -> "11:00 AM - 3:00 PM"
            MealCategory.DINNER -> "3:00 PM - 9:00 PM"
            MealCategory.SNACKING -> "9:00 PM - 4:00 AM"
            null -> ""
        }
    }

    /**
     * Returns the sort order for meal categories in the food log.
     * Lower number = appears earlier in the list.
     *
     * Order: Uncategorized (first), Breakfast, Lunch, Dinner, Snacking
     *
     * @param category The meal category, or null for uncategorized.
     * @return The sort order (0-4).
     */
    fun getCategorySortOrder(category: MealCategory?): Int {
        return when (category) {
            null -> 0                      // Uncategorized first
            MealCategory.BREAKFAST -> 1
            MealCategory.LUNCH -> 2
            MealCategory.DINNER -> 3
            MealCategory.SNACKING -> 4
        }
    }
}
