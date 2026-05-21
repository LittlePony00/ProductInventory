package com.android.rut.miit.productinventory.application.dto.response

import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import java.util.UUID

data class RecipeResponse(
    val id: String? = null,
    val title: String,
    val ingredients: List<RecipeIngredientResponse>,
    val steps: List<String>,
    val time: String,
    val cookingTimeMinutes: Int? = null,
    val calories: Int,
    val caloriesKnown: Boolean = true,
    val source: RecipeSource = RecipeSource.LOCAL_KNOWLEDGE_BASE,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val score: Double = 0.0,
    val usedHouseholdProducts: List<String> = emptyList(),
    val usedExpiringProducts: List<String> = emptyList(),
    val missingIngredients: List<String> = emptyList(),
    val reasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val aiAssisted: Boolean = false,
    val aiGenerated: Boolean = false
)

data class RecipeIngredientResponse(
    val name: String,
    val amount: String
)

data class RecipeIngredientOptionResponse(
    val id: UUID,
    val name: String,
    val categoryName: String?,
    val remainingAmount: Double,
    val unit: QuantityUnit,
    val expiring: Boolean
)
