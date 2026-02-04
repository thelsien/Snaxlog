package com.snaxlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogScreen
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogViewModel
import com.snaxlog.app.ui.theme.SnaxlogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnaxlogTheme {
                val viewModel: DailyFoodLogViewModel = hiltViewModel()
                DailyFoodLogScreen(
                    viewModel = viewModel,
                    onNavigateToGoals = {
                        // Goal management screen - EPIC-002 scope
                    }
                )
            }
        }
    }
}
