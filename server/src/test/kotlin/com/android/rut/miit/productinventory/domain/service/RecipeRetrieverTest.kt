package com.android.rut.miit.productinventory.domain.service

import com.android.rut.miit.productinventory.domain.model.ExpirationDate
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipeRetrieverTest {

    @Test
    fun `retrieves recipes using ingredient names categories expiration and stock`() {
        val riceBowl = document(
            id = "rice-bowl",
            title = "Rice Bowl",
            requiredIngredients = setOf("rice", "vegetables"),
            categories = setOf(ProductCategory.CEREALS, ProductCategory.VEGETABLES_FRUITS)
        )
        val smoothie = document(
            id = "smoothie",
            title = "Smoothie",
            requiredIngredients = setOf("yogurt", "fruit"),
            categories = setOf(ProductCategory.DAIRY, ProductCategory.VEGETABLES_FRUITS)
        )
        val retriever = RecipeRetriever(FakeRecipeKnowledgeRepository(listOf(riceBowl, smoothie)))

        val matches = retriever.retrieve(
            products = listOf(
                product(
                    name = "Greek Yogurt",
                    category = ProductCategory.DAIRY,
                    remainingAmount = 0.2,
                    lowStockThreshold = 0.3,
                    expirationDate = LocalDate.now().plusDays(1)
                ),
                product(name = "Apple Fruit", category = ProductCategory.VEGETABLES_FRUITS),
                product(name = "Rice", category = ProductCategory.CEREALS)
            )
        )

        assertEquals(listOf("smoothie", "rice-bowl"), matches.map { it.document.id })
        assertEquals(listOf("Greek Yogurt", "Apple Fruit"), matches.first().matchedProducts.map { it.name })
    }

    @Test
    fun `ignores unavailable stock`() {
        val retriever = RecipeRetriever(
            FakeRecipeKnowledgeRepository(
                listOf(
                    document(
                        id = "rice-bowl",
                        title = "Rice Bowl",
                        requiredIngredients = setOf("rice"),
                        categories = setOf(ProductCategory.CEREALS)
                    )
                )
            )
        )

        val matches = retriever.retrieve(
            products = listOf(product(name = "Rice", category = ProductCategory.CEREALS, remainingAmount = 0.0))
        )

        assertEquals(emptyList(), matches)
    }

    @Test
    fun `does not retrieve by category alone without ingredient match`() {
        val retriever = RecipeRetriever(
            FakeRecipeKnowledgeRepository(
                listOf(
                    document(
                        id = "fish-vegetables",
                        title = "Fish Vegetables",
                        requiredIngredients = setOf("fish"),
                        categories = setOf(ProductCategory.MEAT_FISH)
                    )
                )
            )
        )

        val matches = retriever.retrieve(
            products = listOf(product(name = "Chicken", category = ProductCategory.MEAT_FISH))
        )

        assertEquals(emptyList(), matches)
    }

    private fun document(
        id: String,
        title: String,
        requiredIngredients: Set<String>,
        categories: Set<ProductCategory>
    ): RecipeDocument =
        RecipeDocument(
            id = id,
            title = title,
            ingredients = requiredIngredients.map { RecipeIngredient(name = it, amount = "1 portion") },
            steps = listOf("Cook"),
            time = "10 minutes",
            calories = 100,
            requiredIngredients = requiredIngredients,
            categories = categories,
            rules = listOf("Use available stock")
        )

    private fun product(
        name: String,
        category: ProductCategory,
        remainingAmount: Double = 1.0,
        lowStockThreshold: Double? = null,
        expirationDate: LocalDate? = null
    ): Product =
        Product(
            name = name,
            category = category,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.let(::ExpirationDate),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )

    private class FakeRecipeKnowledgeRepository(
        private val documents: List<RecipeDocument>
    ) : IRecipeKnowledgeRepository {
        override fun findAll(): List<RecipeDocument> = documents
    }
}
