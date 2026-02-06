package com.snaxlog.app.data.local.database

import androidx.room.TypeConverter
import com.snaxlog.app.data.local.entity.MealCategory

/**
 * Room TypeConverters for custom types.
 * FIP-005: Added MealCategory converter.
 */
class Converters {

    /**
     * Converts MealCategory enum to String for database storage.
     * Returns null if category is null (uncategorized).
     */
    @TypeConverter
    fun fromMealCategory(category: MealCategory?): String? {
        return category?.name
    }

    /**
     * Converts String from database to MealCategory enum.
     * Returns null if value is null or not a valid enum name.
     */
    @TypeConverter
    fun toMealCategory(value: String?): MealCategory? {
        return value?.let {
            try {
                MealCategory.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
