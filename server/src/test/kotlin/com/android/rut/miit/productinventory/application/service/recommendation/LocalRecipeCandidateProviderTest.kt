package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalRecipeCandidateProviderTest {

    @Test
    fun `local recipe knowledge base is disabled by default flag value`() {
        val provider = LocalRecipeCandidateProvider(
            recipeRetriever = RecipeRetriever(FakeRecipeKnowledgeRepository(listOf(document()))),
            enabled = false
        )
        val context = context(product("Rice"))

        assertEquals(emptyList(), provider.findCandidates(context))
        assertEquals(emptyList(), provider.findAnyCandidates(context))
    }

    @Test
    fun `local recipe knowledge base can still be enabled explicitly`() {
        val provider = LocalRecipeCandidateProvider(
            recipeRetriever = RecipeRetriever(FakeRecipeKnowledgeRepository(listOf(document()))),
            enabled = true
        )
        val context = context(product("Rice"))

        assertEquals(listOf("rice"), provider.findCandidates(context).map { it.document.id })
        assertEquals(listOf("rice"), provider.findAnyCandidates(context).map { it.document.id })
    }

    private fun context(product: Product): RecommendationContext =
        RecommendationContext(
            userId = UUID.randomUUID(),
            householdId = product.householdId,
            mode = RecommendationMode.CURRENT_PRODUCTS,
            products = listOf(product),
            expiringProducts = emptyList(),
            preferences = UserFoodPreferences.empty(UUID.randomUUID())
        )

    private fun product(name: String): Product =
        Product(
            name = name,
            category = ProductCategory.CEREALS,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

    private fun document(): RecipeDocument =
        RecipeDocument(
            id = "rice",
            title = "Rice",
            ingredients = listOf(RecipeIngredient("rice", "1 cup")),
            steps = listOf("Cook"),
            time = "10 minutes",
            calories = 100,
            requiredIngredients = setOf("rice"),
            categories = setOf(ProductCategory.CEREALS),
            rules = emptyList()
        )

    private class FakeRecipeKnowledgeRepository(
        private val documents: List<RecipeDocument>
    ) : IRecipeKnowledgeRepository {
        override fun findAll(): List<RecipeDocument> = documents
    }
}
