package com.android.rut.miit.productinventory.domain.model

data class Recipe(
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val calories: Int
)

data class RecipeIngredient(
    val name: String,
    val amount: String
)
