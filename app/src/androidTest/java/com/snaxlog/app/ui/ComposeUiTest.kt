package com.snaxlog.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snaxlog.app.R
import com.snaxlog.app.data.local.entity.CalorieGoalEntity
import com.snaxlog.app.data.local.entity.FoodEntity
import com.snaxlog.app.data.local.entity.FoodIntakeWithFood
import com.snaxlog.app.data.repository.CalorieGoalRepository
import com.snaxlog.app.data.repository.FoodIntakeRepository
import com.snaxlog.app.data.repository.FoodRepository
import com.snaxlog.app.ui.screens.customfood.CreateCustomFoodScreen
import com.snaxlog.app.ui.screens.customfood.CustomFoodListScreen
import com.snaxlog.app.ui.screens.customfood.CustomFoodViewModel
import com.snaxlog.app.ui.screens.dailyfoodlog.AddFoodViewModel
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogScreen
import com.snaxlog.app.ui.screens.dailyfoodlog.DailyFoodLogViewModel
import com.snaxlog.app.ui.screens.dailyfoodlog.EditFoodViewModel
import com.snaxlog.app.ui.screens.goalmanagement.GoalManagementScreen
import com.snaxlog.app.ui.screens.goalmanagement.GoalManagementViewModel
import com.snaxlog.app.ui.theme.SnaxlogTheme
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Clock

/**
 * End-to-Mock Compose UI tests.
 *
 * These tests drive the real Compose UI and navigation but back every ViewModel with
 * MockK-mocked repositories (no real Room database, no emulator-bound data). They cover:
 *  (a) the main daily-food-log screen renders,
 *  (b) navigation main -> goals and back,
 *  (c) navigation main -> custom foods (via the Speed Dial FAB) and back,
 *  (d) the create-custom-food form: filling it in lets save persist the food,
 *  (e) the add-food bottom sheet opens from the FAB.
 *
 * NOTE: These are instrumented tests (app/src/androidTest). assembleDebugAndroidTest compiles
 * them, but running them still requires a connected device/emulator.
 */
class ComposeUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var foodIntakeRepository: FoodIntakeRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var calorieGoalRepository: CalorieGoalRepository

    private val testFood = FoodEntity(
        id = 1, name = "Apple", category = "Fruits",
        servingSize = "1 medium (182g)", servingWeightGrams = 182.0,
        caloriesPerServing = 95, proteinPerServing = 0.5,
        fatPerServing = 0.3, carbsPerServing = 25.1
    )

    private val entriesFlow = MutableStateFlow<List<FoodIntakeWithFood>>(emptyList())
    private val goalFlow = MutableStateFlow<CalorieGoalEntity?>(null)

    @Before
    fun setup() {
        foodIntakeRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)
        calorieGoalRepository = mockk(relaxed = true)

        // Daily log dependencies
        every { foodIntakeRepository.getEntriesForDate(any()) } returns entriesFlow
        every { calorieGoalRepository.getActiveGoal() } returns goalFlow
        every { calorieGoalRepository.getAllGoals() } returns flowOf(emptyList())

        // Add-food / search dependencies
        every { foodRepository.getAllFoods() } returns flowOf(listOf(testFood))
        every { foodRepository.searchFoods(any()) } returns flowOf(listOf(testFood))

        // Custom-food list/create dependencies
        every { foodRepository.getAllUserCreatedFoods() } returns flowOf(emptyList())
    }

    // ============================
    // ViewModel factories (manual construction with mocked repositories)
    // ============================

    private fun dailyFoodLogViewModel() = DailyFoodLogViewModel(
        foodIntakeRepository = foodIntakeRepository,
        calorieGoalRepository = calorieGoalRepository,
        savedStateHandle = SavedStateHandle(),
        clock = Clock.systemDefaultZone()
    )

    private fun addFoodViewModel() = AddFoodViewModel(
        foodIntakeRepository = foodIntakeRepository,
        foodRepository = foodRepository,
        clock = Clock.systemDefaultZone()
    )

    private fun editFoodViewModel() = EditFoodViewModel(
        foodIntakeRepository = foodIntakeRepository,
        clock = Clock.systemDefaultZone()
    )

    private fun goalViewModel() = GoalManagementViewModel(
        calorieGoalRepository = calorieGoalRepository
    )

    private fun customFoodViewModel() = CustomFoodViewModel(
        foodRepository = foodRepository
    )

    /**
     * Hosts a small NavHost that mirrors the production routes "main", "goals" and
     * "customfoods" / "customfood/create" with manually-constructed ViewModels.
     */
    @Composable
    private fun TestNavHost() {
        SnaxlogTheme {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "main") {
                composable("main") {
                    DailyFoodLogScreen(
                        viewModel = dailyFoodLogViewModel(),
                        onNavigateToGoals = { navController.navigate("goals") },
                        onNavigateToCustomFoods = { navController.navigate("customfoods") },
                        addFoodViewModel = addFoodViewModel(),
                        editFoodViewModel = editFoodViewModel()
                    )
                }
                composable("goals") {
                    GoalManagementScreen(
                        viewModel = goalViewModel(),
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("customfoods") {
                    CustomFoodListScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCreateFood = { navController.navigate("customfood/create") },
                        onNavigateToEditFood = {},
                        onNavigateToCreateRecipe = {},
                        onNavigateToEditRecipe = {},
                        viewModel = customFoodViewModel()
                    )
                }
                composable("customfood/create") {
                    CreateCustomFoodScreen(
                        onNavigateBack = { navController.popBackStack() },
                        editFoodId = null,
                        viewModel = customFoodViewModel()
                    )
                }
            }
        }
    }

    private fun str(resId: Int): String = composeTestRule.activity.getString(resId)

    /** Builds a MacroInputField content description: "<Label> field. Enter grams." */
    private fun macroDescription(labelResId: Int): String =
        composeTestRule.activity.getString(R.string.macro_input_description, str(labelResId))

    // ============================
    // (a) Main screen renders
    // ============================

    @Test
    fun mainScreen_rendersDailyFoodLog() {
        composeTestRule.setContent { TestNavHost() }

        // App title and the "My Foods" action are part of the daily log top bar.
        composeTestRule.onNodeWithText(str(R.string.daily_log_app_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(str(R.string.daily_log_add_food)).assertIsDisplayed()
    }

    // ============================
    // (b) Navigate main -> goals and back
    // ============================

    @Test
    fun navigation_mainToGoalsAndBack() {
        composeTestRule.setContent { TestNavHost() }

        // With no active goal, the daily summary card shows a "Set Goal" button that
        // navigates to the goals screen (onNavigateToGoals).
        composeTestRule.onNodeWithText(str(R.string.daily_summary_set_goal)).performClick()

        // The goals screen title is shown.
        composeTestRule.onNodeWithText(str(R.string.goal_management_title)).assertIsDisplayed()

        // Navigate back to the daily log.
        composeTestRule.onNodeWithContentDescription(str(R.string.common_go_back)).performClick()
        composeTestRule.onNodeWithText(str(R.string.daily_log_app_title)).assertIsDisplayed()
    }

    // ============================
    // (c) Navigate main -> custom foods via Speed Dial FAB and back
    // ============================

    @Test
    fun navigation_mainToCustomFoodsAndBack() {
        composeTestRule.setContent { TestNavHost() }

        // Tap the "My Foods" top-bar action to open the custom foods list.
        composeTestRule.onNodeWithContentDescription(str(R.string.daily_log_my_foods)).performClick()

        // Custom foods screen title is shown.
        composeTestRule.onNodeWithText(str(R.string.custom_food_list_title)).assertIsDisplayed()

        // Navigate back to the daily log.
        composeTestRule.onNodeWithContentDescription(str(R.string.common_navigate_back)).performClick()
        composeTestRule.onNodeWithText(str(R.string.daily_log_app_title)).assertIsDisplayed()
    }

    @Test
    fun customFoodsList_speedDialFab_opensCreateOptions() {
        composeTestRule.setContent { TestNavHost() }

        // Open custom foods list.
        composeTestRule.onNodeWithContentDescription(str(R.string.daily_log_my_foods)).performClick()

        // Expand the Speed Dial FAB.
        composeTestRule.onNodeWithContentDescription(str(R.string.custom_food_list_add_menu)).performClick()

        // The "Create custom food" speed-dial option becomes visible.
        composeTestRule.onNodeWithContentDescription(str(R.string.custom_food_list_create_food))
            .assertIsDisplayed()
    }

    // ============================
    // (d) Create custom food: filling the form enables save
    // ============================

    @Test
    fun createCustomFood_fillingFormEnablesSave() {
        composeTestRule.setContent { TestNavHost() }

        // Navigate: main -> custom foods -> create food (via Speed Dial).
        composeTestRule.onNodeWithContentDescription(str(R.string.daily_log_my_foods)).performClick()
        composeTestRule.onNodeWithContentDescription(str(R.string.custom_food_list_add_menu)).performClick()
        composeTestRule.onNodeWithContentDescription(str(R.string.custom_food_list_create_food)).performClick()

        // Create-food screen is shown.
        composeTestRule.onNodeWithText(str(R.string.create_food_title_create)).assertIsDisplayed()

        // Fill in the form via field content descriptions.
        composeTestRule.onNodeWithContentDescription(str(R.string.create_food_name_input_description))
            .performTextInput("Test Food")
        composeTestRule.onNodeWithContentDescription(str(R.string.create_food_serving_amount_description))
            .performTextInput("100")
        composeTestRule.onNodeWithContentDescription(macroDescription(R.string.macro_protein))
            .performScrollTo().performTextInput("10")
        composeTestRule.onNodeWithContentDescription(macroDescription(R.string.macro_fat))
            .performScrollTo().performTextInput("5")
        composeTestRule.onNodeWithContentDescription(macroDescription(R.string.macro_carbs))
            .performScrollTo().performTextInput("20")

        // The Create button is enabled and, when tapped, persists the food via the repository.
        // The form is scrollable, so bring the button into view first.
        composeTestRule.onNodeWithText(str(R.string.create_food_create)).performScrollTo().assertIsEnabled()
        composeTestRule.onNodeWithText(str(R.string.create_food_create)).performClick()

        composeTestRule.runOnIdle { }
        coVerify {
            foodRepository.createCustomFood(
                name = "Test Food",
                servingSizeValue = any(),
                servingUnit = any(),
                protein = any(),
                fat = any(),
                carbs = any()
            )
        }
    }

    // ============================
    // (e) Add-food bottom sheet opens from the FAB
    // ============================

    @Test
    fun addFoodSheet_opensFromFab() {
        composeTestRule.setContent { TestNavHost() }

        // Tap the add-food FAB on the daily log.
        composeTestRule.onNodeWithContentDescription(str(R.string.daily_log_add_food)).performClick()

        // The Add Food bottom sheet content is shown.
        composeTestRule.onNodeWithText(str(R.string.add_food_title)).assertIsDisplayed()
    }
}
