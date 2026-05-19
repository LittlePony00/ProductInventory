package com.android.rut.miit.productinventory.feature.products.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.posix.memcpy

class IosProductImageFileReader : ProductImageFileReader {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun read(path: String): ProductImageFileContent? {
        val data = NSFileManager.defaultManager.contentsAtPath(path) ?: return null
        val length = data.length.toInt()
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        val fileName = path.substringAfterLast('/')
        return ProductImageFileContent(
            bytes = bytes,
            fileName = fileName,
            contentType = fileName.substringAfterLast('.', "").contentType()
        )
    }

    private fun String.contentType(): String =
        when (lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
}
