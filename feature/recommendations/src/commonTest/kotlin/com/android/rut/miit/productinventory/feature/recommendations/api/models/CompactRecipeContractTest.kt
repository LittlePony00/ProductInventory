package com.android.rut.miit.productinventory.feature.recommendations.api.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CompactRecipeContractTest {

    @Test fun `recipe identity is stable for same recipe`() = assertEquals(recipe().localIdentity(), recipe().localIdentity())
    @Test fun `recipe identity changes with id`() = assertDifferent(recipe(id = "1"), recipe(id = "2"))
    @Test fun `recipe identity changes with title`() = assertDifferent(recipe(title = "Rice"), recipe(title = "Soup"))
    @Test fun `recipe identity changes with source`() = assertDifferent(recipe(source = "LOCAL"), recipe(source = "WEB"))
    @Test fun `recipe identity changes with source url`() = assertDifferent(recipe(sourceUrl = "a"), recipe(sourceUrl = "b"))
    @Test fun `recipe identity changes with time`() = assertDifferent(recipe(time = "10 min"), recipe(time = "20 min"))
    @Test fun `recipe identity changes with ingredient name`() = assertDifferent(recipe(ingredients = listOf(ingredient("rice"))), recipe(ingredients = listOf(ingredient("milk"))))
    @Test fun `recipe identity changes with ingredient amount`() = assertDifferent(recipe(ingredients = listOf(ingredient(amount = "1 cup"))), recipe(ingredients = listOf(ingredient(amount = "2 cups"))))
    @Test fun `recipe identity changes with step`() = assertDifferent(recipe(steps = listOf("Cook")), recipe(steps = listOf("Bake")))
    @Test fun `localIdentity extension delegates to property`() = assertEquals(recipe().localIdentity, recipe().localIdentity())

    @Test fun `current products mode name is stable`() = assertEquals("CURRENT_PRODUCTS", RecommendationMode.CURRENT_PRODUCTS.name)
    @Test fun `use soon mode name is stable`() = assertEquals("USE_SOON", RecommendationMode.USE_SOON.name)
    @Test fun `ai generated mode name is stable`() = assertEquals("AI_GENERATED_CUSTOM", RecommendationMode.AI_GENERATED_CUSTOM.name)
    @Test fun `under 30 filter name is stable`() = assertEquals("UNDER_30_MIN", RecipeQuickFilter.UNDER_30_MIN.name)
    @Test fun `few missing filter name is stable`() = assertEquals("FEW_MISSING_INGREDIENTS", RecipeQuickFilter.FEW_MISSING_INGREDIENTS.name)
    @Test fun `ai generated filter name is stable`() = assertEquals("AI_GENERATED", RecipeQuickFilter.AI_GENERATED.name)

    @Test fun `empty search request has no selected products`() = assertTrue(RecipeSearchRequest().selectedProductIds.isEmpty())
    @Test fun `search request preserves selected products`() = assertEquals(setOf("rice", "milk"), RecipeSearchRequest(setOf("rice", "milk")).selectedProductIds)
    @Test fun `ai generation request defaults are empty`() = assertEquals(AiRecipeGenerationRequest(), AiRecipeGenerationRequest())
    @Test fun `ai generation request keeps cooking time`() = assertEquals(30, AiRecipeGenerationRequest(maxCookingTimeMinutes = 30).maxCookingTimeMinutes)
    @Test fun `ai generation request keeps servings`() = assertEquals(4, AiRecipeGenerationRequest(servings = 4).servings)
    @Test fun `ai generation request keeps notes`() = assertEquals("less salt", AiRecipeGenerationRequest(extraNotes = "less salt").extraNotes)

    @Test fun `ingredient option keeps category name`() = assertEquals("Dairy", option(categoryName = "Dairy").categoryName)
    @Test fun `ingredient option keeps remaining amount`() = assertEquals(0.5, option(remainingAmount = 0.5).remainingAmount)
    @Test fun `ingredient option keeps unit`() = assertEquals("GRAMS", option(unit = "GRAMS").unit)
    @Test fun `ingredient option keeps expiring flag`() = assertTrue(option(expiring = true).expiring)
    @Test fun `ingredient option can omit category name`() = assertEquals(null, option(categoryName = null).categoryName)

    @Test fun `ai generated recipe flags are true`() = assertTrue(recipe(aiGenerated = true, aiAssisted = true).aiGenerated && recipe(aiGenerated = true, aiAssisted = true).aiAssisted)
    @Test fun `calories unknown flag is preserved`() = assertFalse(recipe(caloriesKnown = false).caloriesKnown)
    @Test fun `missing ingredients are preserved`() = assertEquals(listOf("milk"), recipe(missingIngredients = listOf("milk")).missingIngredients)
    @Test fun `warnings are preserved`() = assertEquals(listOf("check salt"), recipe(warnings = listOf("check salt")).warnings)
    @Test fun `used expiring products are preserved`() = assertEquals(listOf("yogurt"), recipe(usedExpiringProducts = listOf("yogurt")).usedExpiringProducts)
    @Test fun `load style identity stays unique for large recipe list`() = assertEquals(500, (1..500).map { recipe(title = "Recipe $it").localIdentity() }.toSet().size)

    private fun assertDifferent(left: Recipe, right: Recipe) = assertNotEquals(left.localIdentity(), right.localIdentity())

    private fun ingredient(name: String = "rice", amount: String = "1 cup") = RecipeIngredient(name, amount)

    private fun option(
        categoryName: String? = "Cereals",
        remainingAmount: Double = 1.0,
        unit: String = "PIECES",
        expiring: Boolean = false
    ) = RecipeIngredientOption("product-id", "Rice", categoryName, remainingAmount, unit, expiring)

    private fun recipe(
        id: String? = "recipe-id",
        title: String = "Rice Bowl",
        ingredients: List<RecipeIngredient> = listOf(ingredient()),
        steps: List<String> = listOf("Cook rice"),
        time: String = "15 min",
        caloriesKnown: Boolean = true,
        source: String = "LOCAL_KNOWLEDGE_BASE",
        sourceUrl: String? = null,
        missingIngredients: List<String> = emptyList(),
        usedExpiringProducts: List<String> = emptyList(),
        warnings: List<String> = emptyList(),
        aiAssisted: Boolean = false,
        aiGenerated: Boolean = false
    ) = Recipe(
        id = id,
        title = title,
        ingredients = ingredients,
        steps = steps,
        time = time,
        calories = 300,
        caloriesKnown = caloriesKnown,
        source = source,
        sourceUrl = sourceUrl,
        missingIngredients = missingIngredients,
        usedExpiringProducts = usedExpiringProducts,
        warnings = warnings,
        aiAssisted = aiAssisted,
        aiGenerated = aiGenerated
    )
}
