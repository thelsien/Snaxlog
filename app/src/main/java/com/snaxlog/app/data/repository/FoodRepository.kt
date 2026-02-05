package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow

interface FoodRepository {
    fun getAllFoods(): Flow<List<FoodEntity>>
    fun searchFoods(query: String): Flow<List<FoodEntity>>
    suspend fun getFoodById(id: Long): FoodEntity?
}
