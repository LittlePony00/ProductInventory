package com.android.rut.miit.productinventory.feature.recommendations.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RecipeResponseDto(
    val title: String,
    val ingredients: List<RecipeIngredientDto>,
    val steps: List<String>,
    val time: String,
    val calories: Int
)

@Serializable
data class RecipeIngredientDto(
    val name: String,
    val amount: String
)
