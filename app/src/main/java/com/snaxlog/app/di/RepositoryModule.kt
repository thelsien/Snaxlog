package com.snaxlog.app.di

import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.CalorieGoalRepositoryImpl
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodIntakeRepositoryImpl
import com.snaxlog.app.data.repository.FoodRepository
import com.snaxlog.app.data.repository.FoodRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindFoodIntakeRepository(impl: FoodIntakeRepositoryImpl): FoodIntakeRepository

    @Binds
    @Singleton
    abstract fun bindCalorieGoalRepository(impl: CalorieGoalRepositoryImpl): CalorieGoalRepository
}
