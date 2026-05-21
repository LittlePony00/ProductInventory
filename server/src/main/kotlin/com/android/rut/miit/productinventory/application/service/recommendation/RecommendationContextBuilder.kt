package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.ExpirationStatus
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.model.preferenceCategoryId
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RecommendationContextBuilder(
    private val productRepository: IProductRepository,
    private val membershipRepository: IMembershipRepository,
    private val preferencesRepository: IUserFoodPreferencesRepository,
    private val expirationCheckService: ExpirationCheckService
) {

    fun build(
        userId: UUID,
        householdId: UUID,
        mode: RecommendationMode,
        selectedProductIds: Set<UUID> = emptySet(),
        searchScope: RecipeSearchScope = RecipeSearchScope.STOCK_PRODUCTS
    ): RecommendationContext {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")

        val availableProducts = productRepository.findByHouseholdId(householdId)
            .filter { it.remainingAmount > 0.0 }
        val sortedProducts = expirationCheckService.sortByExpirationPriority(availableProducts)
        val expiringProducts = sortedProducts.filter(Product::isExpiringPriority)

        return RecommendationContext(
            userId = userId,
            householdId = householdId,
            mode = mode,
            products = sortedProducts,
            expiringProducts = expiringProducts,
            preferences = preferencesRepository.findByUserId(userId) ?: UserFoodPreferences.empty(userId),
            selectedProductIds = selectedProductIds,
            searchScope = searchScope
        )
    }
}

enum class RecipeSearchScope {
    STOCK_PRODUCTS,
    ANY_PRODUCTS
}

data class RecommendationContext(
    val userId: UUID,
    val householdId: UUID,
    val mode: RecommendationMode,
    val products: List<Product>,
    val expiringProducts: List<Product>,
    val preferences: UserFoodPreferences,
    val selectedProductIds: Set<UUID> = emptySet(),
    val searchScope: RecipeSearchScope = RecipeSearchScope.STOCK_PRODUCTS
) {
    val allowedProducts: List<Product> =
        products.filterNot { product ->
            val productTerms = product.preferenceTerms()
            product.id in preferences.avoidedProductIds ||
                product.preferenceCategoryId() in preferences.avoidedCategoryIds ||
                preferences.avoidedProducts.any { avoided -> productTerms.matchesAnyPreferenceTerm(ingredientTerms(avoided)) }
        }
    private val allowedProductIds: Set<UUID> = allowedProducts.map(Product::id).toSet()
    val searchesAnyProducts: Boolean = searchScope == RecipeSearchScope.ANY_PRODUCTS

    val candidateProducts: List<Product> = run {
        val modeProducts = when (mode) {
            RecommendationMode.CURRENT_PRODUCTS,
            RecommendationMode.AI_GENERATED_CUSTOM -> allowedProducts
            RecommendationMode.USE_SOON -> expiringProducts.filter { it.id in allowedProductIds }
        }
        when {
            searchesAnyProducts -> emptyList()
            selectedProductIds.isEmpty() -> modeProducts
            else -> modeProducts.filter { it.id in selectedProductIds }
        }
    }
}

private fun Product.isExpiringPriority(): Boolean =
    expirationDate?.status in setOf(ExpirationStatus.EXPIRED, ExpirationStatus.EXPIRING_SOON)

fun Product.preferenceTerms(): Set<String> =
    (listOf(name, category.name) + listOfNotNull(categoryName, ingredientsText))
        .flatMap(::ingredientTerms)
        .toSet()
