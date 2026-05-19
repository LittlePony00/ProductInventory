package com.android.rut.miit.productinventory.feature.barcode.api.models

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.serialization.Serializable

data class BarcodeProductDraft(
    val barcode: String,
    val name: String?,
    val brand: String?,
    val packageQuantity: Double?,
    val packageQuantityUnit: QuantityUnit?,
    val ingredients: String?,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val caloriesKcal: Double?,
    val proteinGrams: Double?,
    val fatGrams: Double?,
    val carbohydratesGrams: Double?,
    val category: ProductCategory?,
    val source: BarcodeProductSource,
    val confidence: Double
)

@Serializable
data class BarcodeRequest(
    val barcode: String
)

@Serializable
data class BarcodeProductDraftResponseDto(
    val barcode: String,
    val name: String? = null,
    val brand: String? = null,
    val packageQuantity: Double? = null,
    val packageQuantityUnit: String? = null,
    val ingredients: String? = null,
    val imageUrl: String? = null,
    val caloriesKcal: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
    val carbohydratesGrams: Double? = null,
    val category: String? = null,
    val source: String,
    val confidence: Double
)

enum class BarcodeProductSource {
    LOCAL_CACHE,
    OPEN_FOOD_FACTS,
    GS1,
    LOCAL_DATABASE,
    UNKNOWN
}

@Serializable
data class BarcodeNotFoundResponse(
    val message: String,
    val barcode: String,
    val needsManualEntry: Boolean
)
