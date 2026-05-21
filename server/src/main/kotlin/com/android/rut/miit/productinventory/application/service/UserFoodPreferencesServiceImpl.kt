package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.EntityNotFoundException
import com.android.rut.miit.productinventory.domain.model.CookingDifficulty
import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import com.android.rut.miit.productinventory.domain.model.FoodPreferenceCategoryOption
import com.android.rut.miit.productinventory.domain.model.FoodPreferenceProductOption
import com.android.rut.miit.productinventory.domain.model.FoodPreferencesOptions
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.model.preferenceCategoryId
import com.android.rut.miit.productinventory.domain.port.inbound.IUserFoodPreferencesService
import com.android.rut.miit.productinventory.domain.port.outbound.ICategoryRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserFoodPreferencesServiceImpl(
    private val preferencesRepository: IUserFoodPreferencesRepository,
    private val userRepository: IUserRepository,
    private val productRepository: IProductRepository,
    private val categoryRepository: ICategoryRepository,
    private val membershipRepository: IMembershipRepository
) : IUserFoodPreferencesService {

    @Transactional(readOnly = true)
    override fun getPreferences(userId: UUID): UserFoodPreferences {
        requireUserExists(userId)
        return preferencesRepository.findByUserId(userId) ?: UserFoodPreferences.empty(userId)
    }

    @Transactional(readOnly = true)
    override fun getPreferenceOptions(userId: UUID, householdId: UUID): FoodPreferencesOptions {
        requireUserExists(userId)
        requireHouseholdMember(userId, householdId)
        val categoriesById = (categoryRepository.findSystemCategories() + categoryRepository.findByHouseholdId(householdId))
            .associateBy { it.id }
        val products = productRepository.findByHouseholdId(householdId)
            .filter { it.remainingAmount > 0.0 }
            .sortedBy { it.name.lowercase() }
            .map { product ->
                FoodPreferenceProductOption(
                    id = product.id,
                    name = product.name,
                    categoryId = product.preferenceCategoryId(),
                    categoryName = product.categoryName
                        ?: categoriesById[product.preferenceCategoryId()]?.name
                        ?: product.category.name
                )
            }
        val categories = categoriesById.values
            .filterNot { it.archived }
            .sortedWith(compareBy({ !it.system }, { it.name.lowercase() }))
            .map {
                FoodPreferenceCategoryOption(
                    id = it.id,
                    name = it.name,
                    system = it.system
                )
            }
        return FoodPreferencesOptions(products = products, categories = categories)
    }

    @Transactional
    override fun updatePreferences(
        userId: UUID,
        preferredCuisines: Collection<String>,
        preferredProducts: Collection<String>,
        dislikedIngredients: Collection<String>,
        avoidedProducts: Collection<String>,
        allergies: Collection<String>,
        dietaryRestrictions: Collection<DietaryRestriction>,
        preferredProductIds: Collection<UUID>,
        avoidedProductIds: Collection<UUID>,
        preferredCategoryIds: Collection<UUID>,
        avoidedCategoryIds: Collection<UUID>,
        maxCookingTimeMinutes: Int?,
        preferredDifficulty: CookingDifficulty?,
        servings: Int?
    ): UserFoodPreferences {
        requireUserExists(userId)
        validatePositive(maxCookingTimeMinutes, "maxCookingTimeMinutes")
        validatePositive(servings, "servings")

        val preferences = UserFoodPreferences(
            userId = userId,
            preferredCuisines = preferredCuisines.sanitizedSet("preferredCuisines"),
            preferredProducts = preferredProducts.sanitizedSet("preferredProducts"),
            dislikedIngredients = dislikedIngredients.sanitizedSet("dislikedIngredients"),
            avoidedProducts = avoidedProducts.sanitizedSet("avoidedProducts"),
            allergies = allergies.sanitizedSet("allergies"),
            dietaryRestrictions = dietaryRestrictions.toSet(),
            preferredProductIds = preferredProductIds.sanitizedUuidSet("preferredProductIds"),
            avoidedProductIds = avoidedProductIds.sanitizedUuidSet("avoidedProductIds"),
            preferredCategoryIds = preferredCategoryIds.sanitizedUuidSet("preferredCategoryIds"),
            avoidedCategoryIds = avoidedCategoryIds.sanitizedUuidSet("avoidedCategoryIds"),
            maxCookingTimeMinutes = maxCookingTimeMinutes,
            preferredDifficulty = preferredDifficulty,
            servings = servings
        )

        return preferencesRepository.save(preferences)
    }

    private fun requireUserExists(userId: UUID) {
        userRepository.findById(userId) ?: throw EntityNotFoundException("User", userId)
    }

    private fun requireHouseholdMember(userId: UUID, householdId: UUID) {
        membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw com.android.rut.miit.productinventory.domain.exception.AccessDeniedException(
                "User is not a member of this household"
            )
    }

    private fun validatePositive(value: Int?, fieldName: String) {
        require(value == null || value > 0) { "$fieldName must be positive" }
    }

    private fun Collection<String>.sanitizedSet(fieldName: String): Set<String> {
        require(size <= MAX_TEXT_ITEMS) { "$fieldName must contain at most $MAX_TEXT_ITEMS items" }
        return asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { value ->
                require(value.length <= MAX_TEXT_LENGTH) { "$fieldName values must be at most $MAX_TEXT_LENGTH characters" }
                value
            }
            .distinctBy { it.lowercase() }
            .toSet()
    }

    private fun Collection<UUID>.sanitizedUuidSet(fieldName: String): Set<UUID> {
        require(size <= MAX_ID_ITEMS) { "$fieldName must contain at most $MAX_ID_ITEMS items" }
        return toSet()
    }

    private companion object {
        const val MAX_TEXT_ITEMS = 30
        const val MAX_TEXT_LENGTH = 64
        const val MAX_ID_ITEMS = 100
    }
}
