package com.snaxlog.app.data.local.entity

/**
 * Meal category classification for food intake entries.
 * FIP-005: Meal Category Classification
 *
 * Time-based auto-assignment rules:
 * - BREAKFAST: 04:00 - 10:59
 * - LUNCH: 11:00 - 14:59
 * - DINNER: 15:00 - 20:59
 * - SNACKING: 21:00 - 03:59
 */
enum class MealCategory {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACKING
}
