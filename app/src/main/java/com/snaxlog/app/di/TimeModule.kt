package com.snaxlog.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Provides the system clock. Injecting [Clock] instead of calling
 * LocalDate.now() / System.currentTimeMillis() directly lets tests use
 * a fixed clock for deterministic date-boundary behavior.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
