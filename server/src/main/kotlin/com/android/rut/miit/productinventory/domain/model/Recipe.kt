package com.android.rut.miit.productinventory.domain.model

data class Recipe(
    val id: String,
    val title: String,
    val description: String,
    val ingredients: List<String>,
    val instructions: String,
    val imageUrl: String? = null
)
