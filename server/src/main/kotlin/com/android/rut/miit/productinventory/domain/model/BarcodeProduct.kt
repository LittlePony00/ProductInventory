package com.android.rut.miit.productinventory.domain.model

import java.time.Instant
import java.util.UUID

data class BarcodeProduct(
    val id: UUID = UUID.randomUUID(),
    val barcode: String,
    val name: String,
    val category: String? = null,
    val imageUrl: String? = null,
    val fetchedAt: Instant = Instant.now()
)
