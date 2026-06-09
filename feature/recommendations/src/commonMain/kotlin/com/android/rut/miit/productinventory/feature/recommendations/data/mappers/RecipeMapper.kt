package com.android.rut.miit.productinventory.feature.recommendations.data.mappers

import com.android.rut.miit.productinventory.feature.recommendations.api.models.Recipe
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredient
import com.android.rut.miit.productinventory.feature.recommendations.api.models.RecipeIngredientOption
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeIngredientOptionDto
import com.android.rut.miit.productinventory.feature.recommendations.data.models.RecipeResponseDto

fun RecipeResponseDto.toDomain() = Recipe(
    id = id,
    title = title,
    ingredients = ingredients.map { RecipeIngredient(name = it.name, amount = it.amount) },
    steps = steps,
    time = time,
    cookingTimeMinutes = cookingTimeMinutes,
    calories = calories,
    caloriesKnown = caloriesKnown,
    source = source,
    sourceName = sourceName,
    sourceUrl = sourceUrl,
    imageUrl = imageUrl,
    score = score,
    usedHouseholdProducts = usedHouseholdProducts,
    usedExpiringProducts = usedExpiringProducts,
    missingIngredients = missingIngredients,
    reasons = reasons,
    warnings = warnings,
    aiAssisted = aiAssisted,
    aiGenerated = aiGenerated
)

fun RecipeIngredientOptionDto.toDomain() = RecipeIngredientOption(
    id = id,
    name = name,
    categoryName = categoryName,
    remainingAmount = remainingAmount,
    unit = unit,
    expiring = expiring
)
