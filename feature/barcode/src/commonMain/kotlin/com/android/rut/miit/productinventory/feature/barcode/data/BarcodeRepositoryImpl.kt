package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeRepository
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraft
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductDraftResponseDto
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeNotFoundResponse
import com.android.rut.miit.productinventory.feature.barcode.api.models.BarcodeProductSource
import com.android.rut.miit.productinventory.feature.barcode.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.ProductResponseDto
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.withTimeoutOrNull

class BarcodeRepositoryImpl(
    private val remoteDataSource: BarcodeRemoteDataSource,
    private val localDataSource: BarcodeLocalDataSource,
    private val productLocalDataSource: ProductLocalDataSource,
    private val remoteLookupTimeoutMillis: Long = REMOTE_LOOKUP_TIMEOUT_MS
) : BarcodeRepository {

    override suspend fun lookupBarcode(householdId: String, barcode: String): BarcodeLookupResult {
        val normalizedBarcode = barcode.trim()
        localDataSource.getCachedBarcode(householdId, normalizedBarcode)?.let {
            return BarcodeLookupResult.DraftFound(it.toDraft())
        }
        productLocalDataSource.getProductByBarcode(householdId, normalizedBarcode)?.let { product ->
            val draft = product.toDraft()
            localDataSource.saveBarcode(draft.toCachedProduct(householdId))
            return BarcodeLookupResult.DraftFound(draft)
        }
        return try {
            val response = if (remoteLookupTimeoutMillis == Long.MAX_VALUE) {
                remoteDataSource.lookupBarcode(householdId, normalizedBarcode)
            } else {
                withTimeoutOrNull(remoteLookupTimeoutMillis) {
                    remoteDataSource.lookupBarcode(householdId, normalizedBarcode)
                } ?: return BarcodeLookupResult.NeedsManualEntry(normalizedBarcode)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val draft = response.body<BarcodeProductDraftResponseDto>().toDomain()
                    localDataSource.saveBarcode(draft.toCachedProduct(householdId))
                    BarcodeLookupResult.DraftFound(draft)
                }
                HttpStatusCode.NotFound -> {
                    response.toManualEntryOrError(normalizedBarcode)
                }
                else -> BarcodeLookupResult.Error("Server error: ${response.status}")
            }
        } catch (e: Exception) {
            BarcodeLookupResult.NeedsManualEntry(normalizedBarcode)
        }
    }

    override suspend fun addBarcodeProduct(householdId: String, barcode: String): BarcodeAddProductResult {
        return try {
            val response = remoteDataSource.addBarcodeProduct(householdId, barcode)

            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val product = response.body<ProductResponseDto>().toDomain()
                    localDataSource.saveBarcode(product.toDraft().toCachedProduct(householdId))
                    BarcodeAddProductResult.ProductAdded(product)
                }
                HttpStatusCode.NotFound -> {
                    when (val lookup = response.toManualEntryOrError(barcode)) {
                        is BarcodeLookupResult.NeedsManualEntry ->
                            BarcodeAddProductResult.NeedsManualEntry(lookup.barcode)
                        is BarcodeLookupResult.Error ->
                            BarcodeAddProductResult.Error(lookup.message)
                        is BarcodeLookupResult.DraftFound ->
                            BarcodeAddProductResult.Error("Unexpected barcode draft response")
                    }
                }
                else -> BarcodeAddProductResult.Error("Server error: ${response.status}")
            }
        } catch (e: Exception) {
            BarcodeAddProductResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun io.ktor.client.statement.HttpResponse.toManualEntryOrError(
        fallbackBarcode: String
    ): BarcodeLookupResult {
        return try {
            val body = body<BarcodeNotFoundResponse>()
            if (body.needsManualEntry) {
                BarcodeLookupResult.NeedsManualEntry(body.barcode)
            } else {
                BarcodeLookupResult.Error(body.message)
            }
        } catch (e: Exception) {
            BarcodeLookupResult.NeedsManualEntry(fallbackBarcode)
        }
    }

    private fun CachedBarcodeProduct.toDraft(): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = barcode,
            name = name,
            brand = brand,
            packageQuantity = packageQuantity,
            packageQuantityUnit = packageQuantityUnit?.let {
                runCatching { com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit.valueOf(it) }
                    .getOrNull()
            },
            ingredients = ingredients,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            caloriesKcal = caloriesKcal,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            carbohydratesGrams = carbohydratesGrams,
            category = category?.let { runCatching { ProductCategory.valueOf(it) }.getOrNull() },
            source = BarcodeProductSource.LOCAL_CACHE,
            confidence = 1.0
        )

    private fun Product.toDraft(): BarcodeProductDraft =
        BarcodeProductDraft(
            barcode = barcode.orEmpty(),
            name = name,
            brand = brand,
            packageQuantity = packageAmount,
            packageQuantityUnit = packageUnit,
            ingredients = ingredientsText,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            caloriesKcal = calories,
            proteinGrams = protein,
            fatGrams = fat,
            carbohydratesGrams = carbs,
            category = category,
            source = BarcodeProductSource.LOCAL_DATABASE,
            confidence = 1.0
        )

    private fun BarcodeProductDraft.toCachedProduct(householdId: String): CachedBarcodeProduct =
        CachedBarcodeProduct(
            householdId = householdId,
            barcode = barcode.trim(),
            name = name,
            brand = brand,
            category = category?.name,
            categoryId = null,
            categoryName = null,
            packageQuantity = packageQuantity,
            packageQuantityUnit = packageQuantityUnit?.name,
            ingredients = ingredients,
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            caloriesKcal = caloriesKcal,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            carbohydratesGrams = carbohydratesGrams,
            source = source.name,
            updatedAt = nowMillis()
        )

    @OptIn(ExperimentalTime::class)
    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        const val REMOTE_LOOKUP_TIMEOUT_MS = 2_000L
    }
}
