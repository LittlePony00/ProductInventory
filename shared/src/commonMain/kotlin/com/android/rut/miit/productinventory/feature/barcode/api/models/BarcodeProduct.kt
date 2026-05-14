package com.android.rut.miit.productinventory.feature.barcode.api.models

import kotlinx.serialization.Serializable

@Serializable
data class BarcodeProduct(
    val barcode: String,
    val name: String,
    val category: String?,
    val imageUrl: String?
)

@Serializable
data class BarcodeRequest(
    val barcode: String
)

@Serializable
data class BarcodeNotFoundResponse(
    val message: String,
    val barcode: String,
    val needsManualEntry: Boolean
)
