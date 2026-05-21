package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecipeSafetyFilterTest {

    private val filter = RecipeSafetyFilter()

    @Test
    fun `excludes allergy matches deterministically`() {
        val result = filter.evaluate(
            recipe = recipe("milk"),
            requiredIngredients = setOf("milk"),
            products = emptyList(),
            preferences = UserFoodPreferences(userId = UUID.randomUUID(), allergies = setOf("молоко"))
        )

        assertFalse(result.safe)
        assertEquals(setOf("молоко"), result.allergyMatches)
    }

    @Test
    fun `excludes strict diet violations`() {
        val result = filter.evaluate(
            recipe = recipe("chicken"),
            requiredIngredients = setOf("chicken"),
            products = emptyList(),
            preferences = UserFoodPreferences(
                userId = UUID.randomUUID(),
                dietaryRestrictions = setOf(DietaryRestriction.VEGETARIAN)
            )
        )

        assertFalse(result.safe)
        assertEquals(setOf(DietaryRestriction.VEGETARIAN), result.dietaryRestrictionViolations)
    }

    @Test
    fun `uses product ingredients text for unsafe matches`() {
        val result = filter.evaluate(
            recipe = recipe("breakfast cereal"),
            requiredIngredients = setOf("cereal"),
            products = listOf(product(name = "Cereal", ingredientsText = "contains peanuts")),
            preferences = UserFoodPreferences(userId = UUID.randomUUID(), allergies = setOf("peanut"))
        )

        assertFalse(result.safe)
        assertEquals(setOf("peanut"), result.allergyMatches)
    }

    @Test
    fun `excludes explicitly avoided products`() {
        val product = product(name = "Milk", ingredientsText = "milk")
        val result = filter.evaluate(
            recipe = recipe("milk"),
            requiredIngredients = setOf("milk"),
            products = listOf(product),
            preferences = UserFoodPreferences(userId = UUID.randomUUID(), avoidedProductIds = setOf(product.id))
        )

        assertFalse(result.safe)
        assertEquals(setOf("Milk"), result.avoidedProductMatches)
    }

    @Test
    fun `excludes free-form avoided product matches`() {
        val result = filter.evaluate(
            recipe = recipe("tomatoes"),
            requiredIngredients = setOf("tomatoes"),
            products = listOf(product(name = "Fresh tomatoes", ingredientsText = "tomatoes")),
            preferences = UserFoodPreferences(userId = UUID.randomUUID(), avoidedProducts = setOf("томат"))
        )

        assertFalse(result.safe)
        assertEquals(setOf("томат"), result.avoidedProductMatches)
    }

    @Test
    fun `excludes explicitly avoided recipe categories`() {
        val result = filter.evaluate(
            recipe = recipe("rice"),
            requiredIngredients = setOf("rice"),
            products = emptyList(),
            recipeCategories = setOf(ProductCategory.CEREALS),
            preferences = UserFoodPreferences(
                userId = UUID.randomUUID(),
                avoidedCategoryIds = setOf(SystemCategoryCatalog.cerealsId)
            )
        )

        assertFalse(result.safe)
        assertEquals(setOf("Крупы и злаки"), result.avoidedCategoryMatches)
    }

    @Test
    fun `excludes avoided system categories by generated ingredient terms`() {
        val result = filter.evaluate(
            recipe = recipe("milk"),
            requiredIngredients = setOf("milk"),
            products = emptyList(),
            preferences = UserFoodPreferences(
                userId = UUID.randomUUID(),
                avoidedCategoryIds = setOf(SystemCategoryCatalog.dairyId)
            )
        )

        assertFalse(result.safe)
        assertEquals(setOf("Молочные продукты"), result.avoidedCategoryMatches)
    }

    private fun recipe(ingredient: String): Recipe =
        Recipe(
            title = "Recipe",
            ingredients = listOf(RecipeIngredient(ingredient, "1 serving")),
            steps = listOf("Cook"),
            time = "10 minutes",
            calories = 100
        )

    private fun product(name: String, ingredientsText: String): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            ingredientsText = ingredientsText,
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
}
