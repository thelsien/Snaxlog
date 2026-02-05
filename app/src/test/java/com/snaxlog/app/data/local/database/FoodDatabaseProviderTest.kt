package com.snaxlog.app.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EPIC-003 / US-011: Comprehensive tests for the pre-loaded food database.
 *
 * Verifies:
 * - AC-048: Foods are organized by category
 * - AC-049: Each food has name, serving size, and calories
 * - AC-051: Database has 500+ items ready on first launch
 * - EC-083: Database contains items suitable for virtualized list
 * - EC-084: No food items have missing critical nutritional data
 */
class FoodDatabaseProviderTest {

    private val allFoods = FoodDatabaseProvider.getAllFoods()

    // ============================================================
    // AC-051: Database has 500+ items
    // ============================================================

    @Test
    fun `AC-051 - database contains at least 500 food items`() {
        assertTrue(
            "Expected at least 500 foods but got ${allFoods.size}",
            allFoods.size >= 500
        )
    }

    @Test
    fun `database contains reasonable number of items (no accidental explosion)`() {
        assertTrue(
            "Expected fewer than 1000 foods but got ${allFoods.size}",
            allFoods.size < 1000
        )
    }

    // ============================================================
    // AC-048: Foods organized by category
    // ============================================================

    @Test
    fun `AC-048 - database contains all required categories`() {
        val categories = allFoods.map { it.category }.distinct().sorted()

        assertTrue("Missing Fruits category", categories.contains("Fruits"))
        assertTrue("Missing Vegetables category", categories.contains("Vegetables"))
        assertTrue("Missing Grains & Cereals category", categories.contains("Grains & Cereals"))
        assertTrue("Missing Protein & Meat category", categories.contains("Protein & Meat"))
        assertTrue("Missing Dairy & Eggs category", categories.contains("Dairy & Eggs"))
        assertTrue("Missing Beverages category", categories.contains("Beverages"))
        assertTrue("Missing Snacks & Sweets category", categories.contains("Snacks & Sweets"))
        assertTrue("Missing Fats & Oils category", categories.contains("Fats & Oils"))
        assertTrue("Missing Legumes & Nuts category", categories.contains("Legumes & Nuts"))
        assertTrue("Missing Restaurant & Fast Food category", categories.contains("Restaurant & Fast Food"))
    }

    @Test
    fun `database has exactly 10 categories`() {
        val categories = allFoods.map { it.category }.distinct()
        assertEquals(10, categories.size)
    }

    @Test
    fun `each category has at least 20 items`() {
        val categoryCounts = allFoods.groupBy { it.category }
            .mapValues { it.value.size }

        categoryCounts.forEach { (category, count) ->
            assertTrue(
                "Category '$category' has only $count items, expected at least 20",
                count >= 20
            )
        }
    }

    // ============================================================
    // AC-049: Each food has name, serving size, and calories
    // ============================================================

    @Test
    fun `AC-049 - all foods have non-empty names`() {
        allFoods.forEachIndexed { index, food ->
            assertTrue(
                "Food at index $index has blank name",
                food.name.isNotBlank()
            )
        }
    }

    @Test
    fun `AC-049 - all foods have non-empty serving sizes`() {
        allFoods.forEachIndexed { index, food ->
            assertTrue(
                "Food '${food.name}' has blank serving size",
                food.servingSize.isNotBlank()
            )
        }
    }

    @Test
    fun `AC-049 - all foods have non-negative calories`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has negative calories: ${food.caloriesPerServing}",
                food.caloriesPerServing >= 0
            )
        }
    }

    @Test
    fun `AC-049 - all foods have positive serving weight in grams`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has non-positive servingWeightGrams: ${food.servingWeightGrams}",
                food.servingWeightGrams > 0
            )
        }
    }

    // ============================================================
    // EC-084: No food items have missing nutritional data fields
    // ============================================================

    @Test
    fun `EC-084 - all foods have non-negative protein values`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has negative protein: ${food.proteinPerServing}",
                food.proteinPerServing >= 0.0
            )
        }
    }

    @Test
    fun `EC-084 - all foods have non-negative fat values`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has negative fat: ${food.fatPerServing}",
                food.fatPerServing >= 0.0
            )
        }
    }

    @Test
    fun `EC-084 - all foods have non-negative carb values`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has negative carbs: ${food.carbsPerServing}",
                food.carbsPerServing >= 0.0
            )
        }
    }

    @Test
    fun `EC-084 - all foods have non-empty categories`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has blank category",
                food.category.isNotBlank()
            )
        }
    }

    // ============================================================
    // Data integrity checks
    // ============================================================

    @Test
    fun `all food names are unique`() {
        val names = allFoods.map { it.name }
        val duplicates = names.groupBy { it }
            .filter { it.value.size > 1 }
            .keys

        assertTrue(
            "Found duplicate food names: $duplicates",
            duplicates.isEmpty()
        )
    }

    @Test
    fun `no food has calories exceeding 1000 per serving (sanity check)`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has unreasonably high calories: ${food.caloriesPerServing}",
                food.caloriesPerServing <= 1000
            )
        }
    }

    @Test
    fun `no food has protein exceeding serving weight (sanity check)`() {
        allFoods.forEach { food ->
            assertTrue(
                "Food '${food.name}' has protein (${food.proteinPerServing}g) exceeding serving weight (${food.servingWeightGrams}g)",
                food.proteinPerServing <= food.servingWeightGrams
            )
        }
    }

    @Test
    fun `all food IDs default to 0 for auto-generation`() {
        allFoods.forEach { food ->
            assertEquals(
                "Food '${food.name}' has non-zero id (should be 0 for autoGenerate)",
                0L, food.id
            )
        }
    }

    @Test
    fun `water has zero calories`() {
        val water = allFoods.find { it.name == "Water" }
        assertTrue("Water not found in database", water != null)
        assertEquals(0, water!!.caloriesPerServing)
        assertEquals(0.0, water.proteinPerServing, 0.01)
        assertEquals(0.0, water.fatPerServing, 0.01)
        assertEquals(0.0, water.carbsPerServing, 0.01)
    }

    @Test
    fun `common foods are present in database`() {
        val expectedFoods = listOf(
            "Apple", "Banana", "Orange",
            "Grilled Chicken Breast", "Salmon (Atlantic)",
            "White Rice (cooked)", "Brown Rice (cooked)",
            "Whole Milk", "Egg (whole, large)",
            "Potato Chips", "Dark Chocolate",
            "Olive Oil", "Peanut Butter",
            "Black Beans (cooked)", "Almonds",
            "Cheeseburger", "Pizza (pepperoni)",
            "Water", "Black Coffee", "Cola"
        )

        val foodNames = allFoods.map { it.name }.toSet()

        expectedFoods.forEach { expected ->
            assertTrue(
                "Expected food '$expected' not found in database",
                foodNames.contains(expected)
            )
        }
    }

    @Test
    fun `serving size strings contain weight information`() {
        // Most serving sizes should contain 'g' or 'ml' for weight/volume info
        val foodsWithWeightInfo = allFoods.count { food ->
            food.servingSize.contains("g)") || food.servingSize.contains("ml)")
                    || food.servingSize.contains("g ") || food.servingSize.contains("ml ")
        }

        // At least 90% should have weight info in serving size string
        val percentage = foodsWithWeightInfo.toDouble() / allFoods.size * 100
        assertTrue(
            "Only ${percentage.toInt()}% of foods have weight info in serving size, expected at least 90%",
            percentage >= 90.0
        )
    }

    // ============================================================
    // Category-specific checks
    // ============================================================

    @Test
    fun `fruits category contains expected items`() {
        val fruits = allFoods.filter { it.category == "Fruits" }
        assertTrue("Fruits category should have 40+ items", fruits.size >= 40)

        val fruitNames = fruits.map { it.name }.toSet()
        assertTrue("Apple missing from fruits", fruitNames.contains("Apple"))
        assertTrue("Banana missing from fruits", fruitNames.contains("Banana"))
        assertTrue("Orange missing from fruits", fruitNames.contains("Orange"))
    }

    @Test
    fun `vegetables category contains expected items`() {
        val vegs = allFoods.filter { it.category == "Vegetables" }
        assertTrue("Vegetables category should have 40+ items", vegs.size >= 40)

        val vegNames = vegs.map { it.name }.toSet()
        assertTrue("Broccoli missing from vegetables", vegNames.contains("Broccoli"))
        assertTrue("Carrot missing from vegetables", vegNames.contains("Carrot"))
    }

    @Test
    fun `protein category contains expected items`() {
        val proteins = allFoods.filter { it.category == "Protein & Meat" }
        assertTrue("Protein & Meat category should have 40+ items", proteins.size >= 40)

        val proteinNames = proteins.map { it.name }.toSet()
        assertTrue("Grilled Chicken Breast missing", proteinNames.contains("Grilled Chicken Breast"))
        assertTrue("Salmon (Atlantic) missing", proteinNames.contains("Salmon (Atlantic)"))
    }

    @Test
    fun `restaurant items category contains expected items`() {
        val restaurant = allFoods.filter { it.category == "Restaurant & Fast Food" }
        assertTrue("Restaurant & Fast Food should have 40+ items", restaurant.size >= 40)

        val names = restaurant.map { it.name }.toSet()
        assertTrue("Cheeseburger missing", names.contains("Cheeseburger"))
        assertTrue("Pizza (pepperoni) missing", names.contains("Pizza (pepperoni)"))
    }

    // ============================================================
    // Nutritional value sanity checks
    // ============================================================

    @Test
    fun `chicken breast is high protein low carb`() {
        val chicken = allFoods.find { it.name == "Grilled Chicken Breast" }!!
        assertTrue("Chicken should have high protein", chicken.proteinPerServing >= 25.0)
        assertTrue("Chicken should have low carbs", chicken.carbsPerServing <= 2.0)
    }

    @Test
    fun `olive oil is high fat zero protein zero carbs`() {
        val oil = allFoods.find { it.name == "Olive Oil" }!!
        assertTrue("Olive oil should have high fat", oil.fatPerServing >= 10.0)
        assertEquals("Olive oil should have zero protein", 0.0, oil.proteinPerServing, 0.01)
        assertEquals("Olive oil should have zero carbs", 0.0, oil.carbsPerServing, 0.01)
    }

    @Test
    fun `beverages category includes both caloric and zero-calorie options`() {
        val beverages = allFoods.filter { it.category == "Beverages" }
        val zeroCal = beverages.count { it.caloriesPerServing == 0 }
        val withCal = beverages.count { it.caloriesPerServing > 0 }

        assertTrue("Should have zero-calorie beverages", zeroCal >= 2)
        assertTrue("Should have caloric beverages", withCal >= 5)
    }

    @Test
    fun `macro calories roughly match total calories for sample foods`() {
        // For each food, protein*4 + fat*9 + carbs*4 should be roughly close to stated calories.
        // Allow 15% tolerance since nutritional data has natural variance.
        val sampleFoods = allFoods.filter {
            it.caloriesPerServing > 20  // Skip very low calorie items where rounding errors dominate
        }.take(100)

        sampleFoods.forEach { food ->
            val calculatedCals = food.proteinPerServing * 4 + food.fatPerServing * 9 + food.carbsPerServing * 4
            val stated = food.caloriesPerServing.toDouble()
            val tolerance = stated * 0.25  // 25% tolerance for USDA rounding

            // This is a soft check - nutritional data inherently has variance
            // We just want to catch egregious data entry errors
            assertFalse(
                "Food '${food.name}' macro-calculated cals (${"%.0f".format(calculatedCals)}) " +
                        "differs significantly from stated cals (${food.caloriesPerServing})",
                kotlin.math.abs(calculatedCals - stated) > tolerance && tolerance > 20
            )
        }
    }
}
