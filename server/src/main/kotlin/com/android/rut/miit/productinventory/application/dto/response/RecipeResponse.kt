package com.android.rut.miit.productinventory.application.dto.response

data class RecipeResponse(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: String,
    val imageUrl: String?
)
