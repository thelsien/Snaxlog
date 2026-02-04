package com.snaxlog.app.data.repository

import com.snaxlog.app.data.local.dao.FoodIntakeEntryDao
import com.snaxlog.app.data.local.entity.FoodIntakeEntryEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodIntakeRepositoryImpl @Inject constructor(
    private val foodIntakeEntryDao: FoodIntakeEntryDao
) : FoodIntakeRepository {

    override fun getEntriesForDate(date: String): Flow<List<FoodIntakeWithFood>> =
        foodIntakeEntryDao.getEntriesForDate(date)

    override suspend fun getEntryWithFoodById(id: Long): FoodIntakeWithFood? =
        foodIntakeEntryDao.getEntryWithFoodById(id)

    override suspend fun addEntry(entry: FoodIntakeEntryEntity): Long =
        foodIntakeEntryDao.insertEntry(entry)

    override suspend fun updateEntry(entry: FoodIntakeEntryEntity) =
        foodIntakeEntryDao.updateEntry(entry)

    override suspend fun deleteEntry(id: Long) =
        foodIntakeEntryDao.deleteEntry(id)

    override fun getTotalCaloriesForDate(date: String): Flow<Int> =
        foodIntakeEntryDao.getTotalCaloriesForDate(date)

    override fun getTotalProteinForDate(date: String): Flow<Double> =
        foodIntakeEntryDao.getTotalProteinForDate(date)

    override fun getTotalFatForDate(date: String): Flow<Double> =
        foodIntakeEntryDao.getTotalFatForDate(date)

    override fun getTotalCarbsForDate(date: String): Flow<Double> =
        foodIntakeEntryDao.getTotalCarbsForDate(date)
}
