package com.snaxlog.app.di

import android.content.Context
import androidx.room.Room
import com.snaxlog.app.data.local.dao.CalorieGoalDao
import com.snaxlog.app.data.local.dao.FoodDao
import com.snaxlog.app.data.local.dao.FoodIntakeEntryDao
import com.snaxlog.app.data.local.dao.RecipeIngredientDao
import com.snaxlog.app.data.local.database.Migrations
import com.snaxlog.app.data.local.database.SnaxlogDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SnaxlogDatabase {
        return Room.databaseBuilder(
            context,
            SnaxlogDatabase::class.java,
            SnaxlogDatabase.DATABASE_NAME
        )
            .addMigrations(
                Migrations.MIGRATION_2_3,
                Migrations.MIGRATION_3_4  // EPIC-006
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFoodDao(database: SnaxlogDatabase): FoodDao =
        database.foodDao()

    @Provides
    fun provideFoodIntakeEntryDao(database: SnaxlogDatabase): FoodIntakeEntryDao =
        database.foodIntakeEntryDao()

    @Provides
    fun provideCalorieGoalDao(database: SnaxlogDatabase): CalorieGoalDao =
        database.calorieGoalDao()

    // EPIC-006: Recipe ingredient DAO
    @Provides
    fun provideRecipeIngredientDao(database: SnaxlogDatabase): RecipeIngredientDao =
        database.recipeIngredientDao()
}
