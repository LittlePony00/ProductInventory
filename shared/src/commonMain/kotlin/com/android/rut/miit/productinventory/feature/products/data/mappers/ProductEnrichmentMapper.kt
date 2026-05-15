package com.android.rut.miit.productinventory.feature.products.data.mappers

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.data.models.ProductEnrichmentSuggestionResponseDto

fun ProductEnrichmentSuggestionResponseDto.toDomain(): ProductEnrichmentSuggestion =
    ProductEnrichmentSuggestion(
        categoryId = categoryId,
        category = runCatching { ProductCategory.valueOf(category) }.getOrDefault(ProductCategory.OTHER),
        categoryName = categoryName,
        confidence = confidence.coerceIn(0.0, 1.0),
        source = runCatching { ProductEnrichmentSource.valueOf(source) }.getOrDefault(ProductEnrichmentSource.FALLBACK),
        suggestedName = suggestedName,
        suggestedBrand = suggestedBrand,
        suggestedIngredientsText = suggestedIngredientsText,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs
    )
