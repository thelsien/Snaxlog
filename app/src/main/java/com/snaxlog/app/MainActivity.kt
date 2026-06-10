package com.snaxlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.snaxlog.app.ui.screens.customfood.CreateCustomFoodScreen
import com.snaxlog.app.ui.screens.customfood.CreateRecipeScreen
import com.snaxlog.app.ui.screens.customfood.CustomFoodListScreen
import com.snaxlog.app.ui.screens.customfood.CustomFoodViewModel
import com.snaxlog.app.ui.screens.customfood.RecipeViewModel
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
                },
                onNavigateToCustomFoods = {
                    navController.navigate("customfoods")
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

        // EPIC-006: Custom Foods and Recipes
        composable("customfoods") {
            val viewModel: CustomFoodViewModel = hiltViewModel()
            CustomFoodListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateFood = { navController.navigate("customfood/create") },
                onNavigateToEditFood = { foodId -> navController.navigate("customfood/$foodId/edit") },
                onNavigateToCreateRecipe = { navController.navigate("recipe/create") },
                onNavigateToEditRecipe = { recipeId -> navController.navigate("recipe/$recipeId/edit") },
                viewModel = viewModel
            )
        }

        composable("customfood/create") {
            val viewModel: CustomFoodViewModel = hiltViewModel()
            CreateCustomFoodScreen(
                onNavigateBack = { navController.popBackStack() },
                editFoodId = null,
                viewModel = viewModel
            )
        }

        composable(
            route = "customfood/{foodId}/edit",
            arguments = listOf(navArgument("foodId") { type = NavType.LongType })
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getLong("foodId")
            val viewModel: CustomFoodViewModel = hiltViewModel()
            CreateCustomFoodScreen(
                onNavigateBack = { navController.popBackStack() },
                editFoodId = foodId,
                viewModel = viewModel
            )
        }

        composable("recipe/create") {
            val viewModel: RecipeViewModel = hiltViewModel()
            CreateRecipeScreen(
                onNavigateBack = { navController.popBackStack() },
                editRecipeId = null,
                viewModel = viewModel
            )
        }

        composable(
            route = "recipe/{recipeId}/edit",
            arguments = listOf(navArgument("recipeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getLong("recipeId")
            val viewModel: RecipeViewModel = hiltViewModel()
            CreateRecipeScreen(
                onNavigateBack = { navController.popBackStack() },
                editRecipeId = recipeId,
                viewModel = viewModel
            )
        }
    }
}
