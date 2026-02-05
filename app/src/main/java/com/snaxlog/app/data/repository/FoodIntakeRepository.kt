package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import kotlinx.coroutines.flow.Flow

interface FoodIntakeRepository {
    fun getEntriesForDate(date: String): Flow<List<FoodIntakeWithFood>>
    suspend fun getEntryWithFoodById(id: Long): FoodIntakeWithFood?
    suspend fun addEntry(entry: FoodIntakeEntryEntity): Long
    suspend fun updateEntry(entry: FoodIntakeEntryEntity)
    suspend fun deleteEntry(id: Long)
    fun getTotalCaloriesForDate(date: String): Flow<Int>
    fun getTotalProteinForDate(date: String): Flow<Double>
    fun getTotalFatForDate(date: String): Flow<Double>
    fun getTotalCarbsForDate(date: String): Flow<Double>
}
