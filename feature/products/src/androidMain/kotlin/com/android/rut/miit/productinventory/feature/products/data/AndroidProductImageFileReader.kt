package com.android.rut.miit.productinventory.feature.products.data

import android.content.Context
import java.io.File

class AndroidProductImageFileReader : ProductImageFileReader {
    override suspend fun read(path: String): ProductImageFileContent? {
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        return ProductImageFileContent(
            bytes = file.readBytes(),
            fileName = file.name,
            contentType = file.extension.contentType()
        )
    }

    private fun String.contentType(): String =
        when (lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
}

class AndroidProductImageLocalCache(
    private val context: Context
) : ProductImageLocalCache {
    override fun localPathForRemoteImage(productId: String, imageUrl: String): String {
        val directory = File(context.filesDir, "product-images/remote")
        return File(directory, remoteProductImageFileName(productId, imageUrl)).absolutePath
    }

    override suspend fun exists(path: String): Boolean =
        File(path).let { it.exists() && it.isFile }

    override suspend fun write(path: String, bytes: ByteArray): Boolean =
        runCatching {
            val file = File(path)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            true
        }.getOrDefault(false)
}
