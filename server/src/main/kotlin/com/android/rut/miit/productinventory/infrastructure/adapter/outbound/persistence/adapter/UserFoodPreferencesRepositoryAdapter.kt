package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.adapter

import com.android.rut.miit.productinventory.domain.model.CookingDifficulty
import com.android.rut.miit.productinventory.domain.model.DietaryRestriction
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity.UserFoodPreferencesEntity
import com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.repository.JpaUserFoodPreferencesRepository
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserFoodPreferencesRepositoryAdapter(
    private val jpaRepository: JpaUserFoodPreferencesRepository,
    private val objectMapper: ObjectMapper
) : IUserFoodPreferencesRepository {

    override fun findByUserId(userId: UUID): UserFoodPreferences? =
        jpaRepository.findById(userId).orElse(null)?.toDomain(objectMapper)

    override fun save(preferences: UserFoodPreferences): UserFoodPreferences =
        jpaRepository.save(preferences.toEntity(objectMapper)).toDomain(objectMapper)
}

private fun UserFoodPreferencesEntity.toDomain(objectMapper: ObjectMapper): UserFoodPreferences =
    UserFoodPreferences(
        userId = userId,
        preferredCuisines = objectMapper.readStringSet(preferredCuisinesJson),
        preferredProducts = objectMapper.readStringSet(preferredProductsJson),
        dislikedIngredients = objectMapper.readStringSet(dislikedIngredientsJson),
        avoidedProducts = objectMapper.readStringSet(avoidedProductsJson),
        allergies = objectMapper.readStringSet(allergiesJson),
        dietaryRestrictions = objectMapper.readStringList(dietaryRestrictionsJson)
            .mapNotNull { runCatching { DietaryRestriction.valueOf(it) }.getOrNull() }
            .toSet(),
        preferredProductIds = objectMapper.readUuidSet(preferredProductIdsJson),
        avoidedProductIds = objectMapper.readUuidSet(avoidedProductIdsJson),
        preferredCategoryIds = objectMapper.readUuidSet(preferredCategoryIdsJson),
        avoidedCategoryIds = objectMapper.readUuidSet(avoidedCategoryIdsJson),
        maxCookingTimeMinutes = maxCookingTimeMinutes,
        preferredDifficulty = preferredDifficulty?.let { runCatching { CookingDifficulty.valueOf(it) }.getOrNull() },
        servings = servings
    )

private fun UserFoodPreferences.toEntity(objectMapper: ObjectMapper): UserFoodPreferencesEntity =
    UserFoodPreferencesEntity(
        userId = userId,
        preferredCuisinesJson = objectMapper.writeValueAsString(preferredCuisines.sorted()),
        preferredProductsJson = objectMapper.writeValueAsString(preferredProducts.sorted()),
        dislikedIngredientsJson = objectMapper.writeValueAsString(dislikedIngredients.sorted()),
        avoidedProductsJson = objectMapper.writeValueAsString(avoidedProducts.sorted()),
        allergiesJson = objectMapper.writeValueAsString(allergies.sorted()),
        dietaryRestrictionsJson = objectMapper.writeValueAsString(dietaryRestrictions.map { it.name }.sorted()),
        preferredProductIdsJson = objectMapper.writeUuidSet(preferredProductIds),
        avoidedProductIdsJson = objectMapper.writeUuidSet(avoidedProductIds),
        preferredCategoryIdsJson = objectMapper.writeUuidSet(preferredCategoryIds),
        avoidedCategoryIdsJson = objectMapper.writeUuidSet(avoidedCategoryIds),
        maxCookingTimeMinutes = maxCookingTimeMinutes,
        preferredDifficulty = preferredDifficulty?.name,
        servings = servings
    )

private fun ObjectMapper.readStringSet(value: String): Set<String> =
    readStringList(value).toSet()

private fun ObjectMapper.readStringList(value: String): List<String> =
    readValue(value, stringListType)

private fun ObjectMapper.readUuidSet(value: String): Set<UUID> =
    readStringList(value)
        .mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
        .toSet()

private fun ObjectMapper.writeUuidSet(value: Set<UUID>): String =
    writeValueAsString(value.map(UUID::toString).sorted())

private val stringListType = object : TypeReference<List<String>>() {}
