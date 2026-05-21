package com.android.rut.miit.productinventory.domain.model

data class FoodPreferencesOptions(
    val products: List<FoodPreferenceProductOption>,
    val categories: List<FoodPreferenceCategoryOption>
)

data class FoodPreferenceProductOption(
    val id: java.util.UUID,
    val name: String,
    val categoryId: java.util.UUID?,
    val categoryName: String?
)

data class FoodPreferenceCategoryOption(
    val id: java.util.UUID,
    val name: String,
    val system: Boolean
)
