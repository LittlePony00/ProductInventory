package com.android.rut.miit.productinventory.domain.model

data class Recipe(
    val title: String,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    val time: String,
    val calories: Int,
    val caloriesKnown: Boolean = true
)

data class RecipeIngredient(
    val name: String,
    val amount: String
)
