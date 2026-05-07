package com.android.rut.miit.productinventory.feature.recommendations.api.models

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: String,
    val imageUrl: String?
)
