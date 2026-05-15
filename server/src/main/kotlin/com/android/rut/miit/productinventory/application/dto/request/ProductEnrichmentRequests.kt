package com.android.rut.miit.productinventory.application.dto.request

data class ProductEnrichmentSuggestionRequest(
    val name: String? = null,
    val brand: String? = null,
    val barcode: String? = null,
    val ingredientsText: String? = null
)
