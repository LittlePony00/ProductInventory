package com.android.rut.miit.productinventory.feature.products.data

data class ProductImageFileContent(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)

interface ProductImageFileReader {
    suspend fun read(path: String): ProductImageFileContent?
}

object NoopProductImageFileReader : ProductImageFileReader {
    override suspend fun read(path: String): ProductImageFileContent? = null
}
