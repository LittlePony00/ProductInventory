package com.android.rut.miit.productinventory.domain.service

import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import java.util.Locale

class RecipeRetriever(
    private val recipeKnowledgeRepository: IRecipeKnowledgeRepository
) {

    fun retrieve(products: List<Product>, limit: Int = DEFAULT_LIMIT): List<RecipeDocumentMatch> {
        val availableProducts = products
            .filter { it.remainingAmount > 0.0 }
            .distinctBy { normalize(it.name) }

        if (availableProducts.isEmpty()) return emptyList()

        return recipeKnowledgeRepository.findAll()
            .asSequence()
            .map { document ->
                val ingredientMatchedProducts = availableProducts.filter { product ->
                    document.requiredIngredients.any { required ->
                        normalize(product.name).matchesIngredient(required)
                    }
                }
                RecipeDocumentMatch(
                    document = document,
                    score = score(
                        documentRequired = document.requiredIngredients,
                        matchedProducts = ingredientMatchedProducts,
                        documentCategoryCount = document.categories.size
                    ),
                    matchedProducts = ingredientMatchedProducts,
                    appliedRules = document.rules
                )
            }
            .filter { it.score > 0.0 }
            .sortedWith(compareByDescending<RecipeDocumentMatch> { it.score }.thenBy { it.document.title })
            .take(limit)
            .toList()
    }

    private fun score(
        documentRequired: Set<String>,
        matchedProducts: List<Product>,
        documentCategoryCount: Int
    ): Double {
        if (matchedProducts.isEmpty()) return 0.0

        val matchedNames = matchedProducts.map { normalize(it.name) }
        val requiredCoverage = documentRequired.map(::normalize).count { required ->
            matchedNames.any { it.matchesIngredient(required) }
        }.toDouble() / documentRequired.size.coerceAtLeast(1)

        val expirationBoost = matchedProducts.sumOf { product ->
            when (product.expirationDate?.status) {
                ExpirationStatus.EXPIRED -> 8.0
                ExpirationStatus.EXPIRING_SOON -> 14.0
                ExpirationStatus.FRESH -> 3.0
                ExpirationStatus.UNKNOWN, null -> 0.0
            }
        }
        val stockBoost = matchedProducts.sumOf { product ->
            val threshold = product.lowStockThreshold
            when {
                threshold != null && product.remainingAmount <= threshold -> 8.0
                product.remainingAmount >= product.quantity.value -> 4.0
                else -> 2.0
            }
        }
        val categoryCoverage = matchedProducts
            .map { it.category }
            .distinct()
            .count()
            .toDouble() / documentCategoryCount.coerceAtLeast(1)

        return requiredCoverage * 100.0 + expirationBoost + stockBoost + categoryCoverage * 12.0
    }

    private fun String.matchesIngredient(required: String): Boolean =
        contains(required) || required.contains(this)

    private fun normalize(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    private companion object {
        const val DEFAULT_LIMIT = 3
    }
}
