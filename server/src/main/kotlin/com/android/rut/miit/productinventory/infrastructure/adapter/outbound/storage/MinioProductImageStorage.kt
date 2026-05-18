package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.storage

import com.android.rut.miit.productinventory.domain.port.outbound.IProductImageStorage
import com.android.rut.miit.productinventory.domain.port.outbound.StoredProductImage
import io.minio.BucketExistsArgs
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import io.minio.errors.ErrorResponseException
import io.minio.http.Method
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

@Component
class MinioProductImageStorage(
    @Value("\${product-images.s3.endpoint:}") endpoint: String,
    @Value("\${product-images.s3.access-key:}") accessKey: String,
    @Value("\${product-images.s3.secret-key:}") secretKey: String,
    @param:Value("\${product-images.s3.bucket:product-images}") private val bucket: String,
    @param:Value("\${product-images.s3.public-base-url:}") private val publicBaseUrl: String,
    @param:Value("\${product-images.s3.presigned-expiry-days:7}") private val presignedExpiryDays: Int
) : IProductImageStorage {

    private val client: MinioClient? = if (
        endpoint.isNotBlank() &&
        accessKey.isNotBlank() &&
        secretKey.isNotBlank()
    ) {
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    } else {
        null
    }

    override fun uploadProductImage(
        householdId: UUID,
        productId: UUID,
        originalFilename: String?,
        contentType: String?,
        size: Long,
        inputStream: InputStream
    ): StoredProductImage {
        val minio = client ?: throw IllegalStateException("Product image S3 storage is not configured")
        ensureBucket(minio)
        val objectName = "products/$householdId/$productId/${UUID.randomUUID()}${extension(originalFilename, contentType)}"
        minio.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .contentType(contentType?.takeIf(String::isNotBlank) ?: DEFAULT_CONTENT_TYPE)
                .stream(inputStream, size, PART_SIZE)
                .build()
        )
        return StoredProductImage(
            url = publicUrlOrPresigned(minio, objectName),
            objectKey = objectName
        )
    }

    override fun findBarcodeImageUrl(barcode: String): String? {
        val minio = client ?: return null
        return BARCODE_EXTENSIONS
            .asSequence()
            .map { "barcodes/${barcode.trim()}.$it" }
            .firstOrNull { objectName -> objectExists(minio, objectName) }
            ?.let { objectName -> publicUrlOrPresigned(minio, objectName) }
    }

    override fun deleteObject(objectKey: String) {
        val minio = client ?: return
        if (objectKey.isBlank()) return
        runCatching {
            minio.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectKey)
                    .build()
            )
        }
    }

    private fun ensureBucket(minio: MinioClient) {
        val exists = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
        if (!exists) {
            minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    private fun objectExists(minio: MinioClient, objectName: String): Boolean =
        runCatching {
            minio.statObject(StatObjectArgs.builder().bucket(bucket).`object`(objectName).build())
            true
        }.getOrElse { error ->
            if (error is ErrorResponseException && error.errorResponse().code() == "NoSuchKey") {
                false
            } else {
                false
            }
        }

    private fun publicUrlOrPresigned(minio: MinioClient, objectName: String): String {
        val baseUrl = publicBaseUrl.trim().trimEnd('/')
        if (baseUrl.isNotBlank()) return "$baseUrl/$objectName"
        return minio.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .`object`(objectName)
                .expiry(presignedExpiryDays.coerceIn(1, 7), TimeUnit.DAYS)
                .build()
        )
    }

    private fun extension(originalFilename: String?, contentType: String?): String =
        originalFilename
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it in BARCODE_EXTENSIONS }
            ?.let { ".$it" }
            ?: when (contentType?.lowercase()) {
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                else -> ".jpg"
            }

    private companion object {
        const val DEFAULT_CONTENT_TYPE = "image/jpeg"
        const val PART_SIZE = 5L * 1024L * 1024L
        val BARCODE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
