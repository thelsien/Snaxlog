package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.entity.FoodEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepositoryImpl @Inject constructor(
    private val foodDao: FoodDao
) : FoodRepository {

    override fun getAllFoods(): Flow<List<FoodEntity>> =
        foodDao.getAllFoods()

    override fun searchFoods(query: String): Flow<List<FoodEntity>> =
        foodDao.searchFoods(query)

    override suspend fun getFoodById(id: Long): FoodEntity? =
        foodDao.getFoodById(id)
}
