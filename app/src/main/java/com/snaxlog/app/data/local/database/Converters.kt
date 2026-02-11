package com.snaxlog.app.data.local.database

import androidx.room.TypeConverter
import com.snaxlog.app.data.local.entity.FoodType
import com.snaxlog.app.data.local.entity.MealCategory
import com.snaxlog.app.data.local.entity.ServingUnit

/**
 * Room TypeConverters for custom types.
 * FIP-005: Added MealCategory converter.
 * EPIC-006: Added FoodType and ServingUnit converters.
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

    // EPIC-006: FoodType converters

    /**
     * Converts FoodType enum to String for database storage.
     */
    @TypeConverter
    fun fromFoodType(foodType: FoodType): String {
        return foodType.name
    }

    /**
     * Converts String from database to FoodType enum.
     * Defaults to PREDEFINED if value is invalid.
     */
    @TypeConverter
    fun toFoodType(value: String): FoodType {
        return try {
            FoodType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            FoodType.PREDEFINED
        }
    }

    // EPIC-006: ServingUnit converters

    /**
     * Converts ServingUnit enum to String for database storage.
     */
    @TypeConverter
    fun fromServingUnit(unit: ServingUnit): String {
        return unit.name
    }

    /**
     * Converts String from database to ServingUnit enum.
     * Defaults to GRAM if value is invalid.
     */
    @TypeConverter
    fun toServingUnit(value: String): ServingUnit {
        return try {
            ServingUnit.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ServingUnit.GRAM
        }
    }
}
