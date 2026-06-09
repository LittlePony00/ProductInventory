package com.android.rut.miit.productinventory.feature.products.data

data class ProductImageFileContent(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)

interface ProductImageFileReader {
    suspend fun read(path: String): ProductImageFileContent?
}

interface ProductImageLocalCache {
    fun localPathForRemoteImage(productId: String, imageUrl: String): String?
    suspend fun exists(path: String): Boolean
    suspend fun write(path: String, bytes: ByteArray): Boolean
}

internal fun remoteProductImageFileName(productId: String, imageUrl: String): String =
    "${productId}-${stableImageUrlHash(imageUrl)}.${imageUrl.imageExtension()}"

private fun stableImageUrlHash(value: String): String =
    value.hashCode().toUInt().toString(radix = 16)

private fun String.imageExtension(): String {
    val extension = substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
    return extension.takeIf { it in supportedImageExtensions } ?: "jpg"
}

private val supportedImageExtensions = setOf("jpg", "jpeg", "png", "webp")
