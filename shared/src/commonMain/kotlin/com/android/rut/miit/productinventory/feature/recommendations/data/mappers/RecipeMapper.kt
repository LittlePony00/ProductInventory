package com.android.rut.miit.productinventory.feature.recommendations.data.mappers

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto

fun RecipeResponseDto.toDomain() = Recipe(
    id = id,
    title = title,
    description = description,
    ingredients = ingredients,
    instructions = instructions,
    imageUrl = imageUrl
)
