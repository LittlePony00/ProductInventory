package com.android.rut.miit.productinventory.feature.recommendations.data.mappers

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto

fun RecipeResponseDto.toDomain() = Recipe(
    title = title,
    ingredients = ingredients.map { RecipeIngredient(name = it.name, amount = it.amount) },
    steps = steps,
    time = time,
    calories = calories
)
