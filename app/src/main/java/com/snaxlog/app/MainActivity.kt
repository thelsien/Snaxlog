package com.snaxlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogScreen
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogViewModel
import com.snaxlog.app.ui.screens.goalmanagement.GoalManagementScreen
import com.snaxlog.app.ui.screens.goalmanagement.GoalManagementViewModel
import com.snaxlog.app.ui.theme.SnaxlogTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SnaxlogTheme {
                SnaxlogNavHost()
            }
        }
    }
}

@Composable
private fun SnaxlogNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") {
            val viewModel: DailyFoodLogViewModel = hiltViewModel()
            DailyFoodLogScreen(
                viewModel = viewModel,
                onNavigateToGoals = {
                    navController.navigate("goals")
                }
            )
        }

        composable("goals") {
            val viewModel: GoalManagementViewModel = hiltViewModel()
            GoalManagementScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
