package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.AiProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.domain.model.Category
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentCategoryOption
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentInput
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentSource
import com.android.rut.miit.productinventory.domain.model.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.port.inbound.IProductEnrichmentService
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductEnrichmentClient
import com.android.rut.miit.productinventory.domain.service.ProductCategoryRuleMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductEnrichmentServiceImpl(
    private val membershipRepository: IMembershipRepository,
    private val categoryRepository: ICategoryRepository,
    private val productEnrichmentClient: IProductEnrichmentClient,
    private val ruleMatcher: ProductCategoryRuleMatcher
) : IProductEnrichmentService {

    @Transactional(readOnly = true)
    override fun suggestProduct(
        userId: UUID,
        householdId: UUID,
        input: ProductEnrichmentInput
    ): ProductEnrichmentSuggestion {
        requireMembership(userId, householdId)
        val categories = availableCategories(householdId)
        val options = categories.map {
            ProductEnrichmentCategoryOption(
                id = it.id,
                code = it.code,
                name = it.name,
                system = it.system
            )
        }

        return productEnrichmentClient.suggestProduct(input, options)
            ?.toSuggestion(categories)
            ?: ruleMatcher.suggestCategory(input)?.let { rule ->
                val category = categories.resolve(rule.category)
                ProductEnrichmentSuggestion(
                    categoryId = category.id,
                    category = category.code ?: rule.category,
                    categoryName = category.name,
                    confidence = rule.confidence,
                    source = ProductEnrichmentSource.RULE_BASED
                )
            }
            ?: fallbackSuggestion(categories)
    }

    private fun AiProductEnrichmentSuggestion.toSuggestion(categories: List<Category>): ProductEnrichmentSuggestion? {
        val category = when {
            categoryId != null -> categories.firstOrNull { it.id == categoryId }
            category != null -> categories.resolve(category)
            !categoryName.isNullOrBlank() -> categories.firstOrNull { it.name.equals(categoryName, ignoreCase = true) }
            else -> null
        } ?: return null

        return ProductEnrichmentSuggestion(
            categoryId = category.id,
            category = category.code ?: this.category ?: ProductCategory.OTHER,
            categoryName = category.name,
            confidence = confidence?.coerceIn(0.0, 1.0) ?: 0.65,
            source = ProductEnrichmentSource.GIGACHAT,
            suggestedName = suggestedName?.trimToNull(),
            suggestedBrand = suggestedBrand?.trimToNull(),
            suggestedIngredientsText = suggestedIngredientsText?.trimToNull(),
            calories = calories?.takeIf { it >= 0.0 },
            protein = protein?.takeIf { it >= 0.0 },
            fat = fat?.takeIf { it >= 0.0 },
            carbs = carbs?.takeIf { it >= 0.0 }
        )
    }

    private fun availableCategories(householdId: UUID): List<Category> =
        categoryRepository.findSystemCategories(includeArchived = false) +
            categoryRepository.findByHouseholdId(householdId, includeArchived = false)

    private fun fallbackSuggestion(categories: List<Category>): ProductEnrichmentSuggestion {
        val category = categories.resolve(ProductCategory.OTHER)
        return ProductEnrichmentSuggestion(
            categoryId = category.id,
            category = category.code ?: ProductCategory.OTHER,
            categoryName = category.name,
            confidence = 0.2,
            source = ProductEnrichmentSource.FALLBACK
        )
    }

    private fun List<Category>.resolve(category: ProductCategory): Category =
        firstOrNull { it.code == category }
            ?: first { it.id == SystemCategoryCatalog.idFor(category) }

    private fun requireMembership(userId: UUID, householdId: UUID) {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")
    }
}

private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }
