package com.android.rut.miit.productinventory.feature.profile.data.mappers

import com.android.rut.miit.productinventory.feature.profile.api.models.FoodPreferences
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileMapperTest {

    @Test
    fun `normalizes enum-backed food preference fields for server requests`() {
        val request = FoodPreferences(
            dietaryRestrictions = setOf("vegan", "gluten_free"),
            preferredProducts = setOf("buckwheat", "tomato"),
            avoidedProducts = setOf("mayo"),
            preferredProductIds = setOf("rice-id"),
            avoidedProductIds = setOf("milk-id"),
            preferredCategoryIds = setOf("cereal-id"),
            avoidedCategoryIds = setOf("dairy-id"),
            preferredDifficulty = "easy"
        ).toRequestDto()

        assertEquals(setOf("VEGAN", "GLUTEN_FREE"), request.dietaryRestrictions)
        assertEquals(setOf("buckwheat", "tomato"), request.preferredProducts)
        assertEquals(setOf("mayo"), request.avoidedProducts)
        assertEquals(setOf("rice-id"), request.preferredProductIds)
        assertEquals(setOf("milk-id"), request.avoidedProductIds)
        assertEquals(setOf("cereal-id"), request.preferredCategoryIds)
        assertEquals(setOf("dairy-id"), request.avoidedCategoryIds)
        assertEquals("EASY", request.preferredDifficulty)
    }
}
