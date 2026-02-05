package com.snaxlog.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a calorie goal -- either pre-defined or user-created.
 * Only one goal can be active at a time.
 */
@Entity(tableName = "calorie_goals")
data class CalorieGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val calorieTarget: Int,
    val proteinTarget: Double? = null,
    val fatTarget: Double? = null,
    val carbsTarget: Double? = null,
    val isActive: Boolean = false,
    val isPredefined: Boolean = false
)
