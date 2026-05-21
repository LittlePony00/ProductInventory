package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "user_food_preferences")
class UserFoodPreferencesEntity(
    @Id
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),

    @Column(name = "preferred_cuisines_json", nullable = false, columnDefinition = "TEXT")
    var preferredCuisinesJson: String = "[]",

    @Column(name = "preferred_products_json", nullable = false, columnDefinition = "TEXT")
    var preferredProductsJson: String = "[]",

    @Column(name = "disliked_ingredients_json", nullable = false, columnDefinition = "TEXT")
    var dislikedIngredientsJson: String = "[]",

    @Column(name = "avoided_products_json", nullable = false, columnDefinition = "TEXT")
    var avoidedProductsJson: String = "[]",

    @Column(name = "allergies_json", nullable = false, columnDefinition = "TEXT")
    var allergiesJson: String = "[]",

    @Column(name = "dietary_restrictions_json", nullable = false, columnDefinition = "TEXT")
    var dietaryRestrictionsJson: String = "[]",

    @Column(name = "preferred_product_ids_json", nullable = false, columnDefinition = "TEXT")
    var preferredProductIdsJson: String = "[]",

    @Column(name = "avoided_product_ids_json", nullable = false, columnDefinition = "TEXT")
    var avoidedProductIdsJson: String = "[]",

    @Column(name = "preferred_category_ids_json", nullable = false, columnDefinition = "TEXT")
    var preferredCategoryIdsJson: String = "[]",

    @Column(name = "avoided_category_ids_json", nullable = false, columnDefinition = "TEXT")
    var avoidedCategoryIdsJson: String = "[]",

    @Column(name = "max_cooking_time_minutes")
    var maxCookingTimeMinutes: Int? = null,

    @Column(name = "preferred_difficulty")
    var preferredDifficulty: String? = null,

    @Column
    var servings: Int? = null
)
