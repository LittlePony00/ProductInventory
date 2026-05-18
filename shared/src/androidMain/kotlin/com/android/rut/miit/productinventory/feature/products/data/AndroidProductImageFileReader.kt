package com.android.rut.miit.productinventory.feature.products.data

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
