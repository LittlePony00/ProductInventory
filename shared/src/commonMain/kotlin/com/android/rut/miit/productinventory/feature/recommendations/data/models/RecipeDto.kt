package com.android.rut.miit.productinventory.feature.recommendations.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RecipeResponseDto(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: String,
    val imageUrl: String? = null
)
