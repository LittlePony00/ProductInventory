package com.android.rut.miit.productinventory.feature.products.api

class SuggestProductEnrichmentUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(
        householdId: String,
        name: String?,
        brand: String?,
        barcode: String?,
        ingredientsText: String?
    ) = repository.suggestProductEnrichment(
        householdId = householdId,
        name = name,
        brand = brand,
        barcode = barcode,
        ingredientsText = ingredientsText
    )
}
