package com.android.rut.miit.productinventory.feature.products.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
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

class IosProductImageLocalCache : ProductImageLocalCache {
    override fun localPathForRemoteImage(productId: String, imageUrl: String): String? {
        val baseDirectory = (NSFileManager.defaultManager
            .URLsForDirectory(NSApplicationSupportDirectory, NSUserDomainMask)
            .firstOrNull() as? NSURL)
            ?.path
            ?: return null
        return "$baseDirectory/product-images/remote/${remoteProductImageFileName(productId, imageUrl)}"
    }

    override suspend fun exists(path: String): Boolean =
        NSFileManager.defaultManager.fileExistsAtPath(path)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun write(path: String, bytes: ByteArray): Boolean =
        runCatching {
            val directory = path.substringBeforeLast('/', missingDelimiterValue = "")
            if (directory.isNotBlank()) {
                NSFileManager.defaultManager.createDirectoryAtPath(
                    path = directory,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }
            val file = fopen(path, "wb") ?: return@runCatching false
            try {
                bytes.usePinned { pinned ->
                    fwrite(pinned.addressOf(0), 1u, bytes.size.toULong(), file) == bytes.size.toULong()
                }
            } finally {
                fclose(file)
            }
        }.getOrDefault(false)
}
