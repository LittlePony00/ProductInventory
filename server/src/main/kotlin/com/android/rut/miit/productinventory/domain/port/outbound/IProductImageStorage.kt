package com.android.rut.miit.productinventory.domain.port.outbound

import java.io.InputStream
import java.util.UUID

data class StoredProductImage(
    val url: String,
    val objectKey: String
)

interface IProductImageStorage {
    fun uploadProductImage(
        householdId: UUID,
        productId: UUID,
        originalFilename: String?,
        contentType: String?,
        size: Long,
        inputStream: InputStream
    ): StoredProductImage

    fun findBarcodeImageUrl(barcode: String): String?

    fun deleteObject(objectKey: String)
}
