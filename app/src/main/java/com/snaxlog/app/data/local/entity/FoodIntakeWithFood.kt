package com.snaxlog.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Combines a food intake entry with its associated food item data.
 * Used to display entries with full food details without separate queries.
 */
data class FoodIntakeWithFood(
    @Embedded
    val entry: FoodIntakeEntryEntity,
    @Relation(
        parentColumn = "foodId",
        entityColumn = "id"
    )
    val food: FoodEntity
)
