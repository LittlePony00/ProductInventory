package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.port.inbound.IRecommendationService
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeProvider
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RecommendationServiceImpl(
    private val productRepository: IProductRepository,
    private val membershipRepository: IMembershipRepository,
    private val recipeProvider: IRecipeProvider,
    private val expirationCheckService: ExpirationCheckService
) : IRecommendationService {

    @Transactional(readOnly = true)
    override fun getRecipes(userId: UUID, householdId: UUID): List<Recipe> {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")

        val products = productRepository.findByHouseholdId(householdId)
        val sortedProducts = expirationCheckService.sortByExpirationPriority(products)
        return recipeProvider.findRecipes(RecipeGenerationRequest(sortedProducts))
    }
}
