package com.snaxlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a food intake log entry -- a record that the user ate
 * a certain number of servings of a food on a given date/time.
 *
 * FIP-005: Added mealCategory field for meal classification.
 */
@Entity(
    tableName = "food_intake_entries",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("foodId"), Index("date")]
)
data class FoodIntakeEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodId: Long,
    val servings: Double,
    val totalCalories: Int,
    val totalProtein: Double,
    val totalFat: Double,
    val totalCarbs: Double,
    val date: String,       // ISO date string yyyy-MM-dd for the day
    val timestamp: Long,    // epoch millis for ordering
    /**
     * FIP-005: Meal category classification.
     * Nullable - null means uncategorized.
     * Stored as String in database (enum name).
     */
    val mealCategory: MealCategory? = null
)
