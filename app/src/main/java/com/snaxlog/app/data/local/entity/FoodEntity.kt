package com.snaxlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a food item in the pre-loaded food database.
 * These are read-only reference data used when logging food intake.
 */
@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val servingSize: String,
    val servingWeightGrams: Double,
    val caloriesPerServing: Int,
    val proteinPerServing: Double,
    val fatPerServing: Double,
    val carbsPerServing: Double
)
