package com.android.rut.miit.productinventory.application.dto.response

data class RecipeResponse(
    val title: String,
    val ingredients: List<RecipeIngredientResponse>,
    val steps: List<String>,
    val time: String,
    val calories: Int
)

data class RecipeIngredientResponse(
    val name: String,
    val amount: String
)
