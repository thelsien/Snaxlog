package com.snaxlog.app

import android.app.Application
import com.snaxlog.app.data.local.database.DatabaseInitializer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SnaxlogApplication : Application() {

    @Inject
    lateinit var databaseInitializer: DatabaseInitializer

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Seed database with dummy foods and predefined goals on first launch
        applicationScope.launch {
            databaseInitializer.initializeIfNeeded()
        }
    }
}
