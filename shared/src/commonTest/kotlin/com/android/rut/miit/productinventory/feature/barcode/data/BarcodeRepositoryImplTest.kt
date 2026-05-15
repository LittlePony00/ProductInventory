package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class BarcodeRepositoryImplTest {

    @Test
    fun `lookup barcode returns draft from barcode draft endpoint`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/barcodes/4601234567890", request.url.encodedPath)
            respondJson(
                content = """
                    {
                      "barcode": "4601234567890",
                      "name": "Milk",
                      "brand": "Brand",
                      "packageQuantity": 950.0,
                      "packageQuantityUnit": "MILLILITERS",
                      "ingredients": "Milk",
                      "caloriesKcal": 60.0,
                      "proteinGrams": 3.0,
                      "fatGrams": 2.5,
                      "carbohydratesGrams": 4.7,
                      "category": "DAIRY",
                      "source": "OPEN_FOOD_FACTS",
                      "confidence": 0.82
                    }
                """.trimIndent()
            )
        }
        val localDataSource = FakeBarcodeLocalDataSource()

        val result = repository(engine, localDataSource).lookupBarcode("household-id", "4601234567890")

        val found = assertIs<BarcodeLookupResult.DraftFound>(result)
        assertEquals("Milk", found.draft.name)
        assertEquals(ProductCategory.DAIRY, found.draft.category)
        assertEquals("Milk", localDataSource.saved.single().name)
    }

    @Test
    fun `lookup barcode returns manual entry on not found response`() = runTest {
        val engine = MockEngine {
            respondJson(
                content = """
                    {
                      "message": "Barcode not found",
                      "barcode": "4601234567890",
                      "needsManualEntry": true
                    }
                """.trimIndent(),
                status = HttpStatusCode.NotFound
            )
        }

        val result = repository(engine).lookupBarcode("household-id", "4601234567890")

        val manualEntry = assertIs<BarcodeLookupResult.NeedsManualEntry>(result)
        assertEquals("4601234567890", manualEntry.barcode)
    }

    @Test
    fun `add barcode product posts barcode and returns created product`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/households/household-id/products/barcode", request.url.encodedPath)
            respondJson(
                content = """
                    {
                      "id": "product-id",
                      "name": "Milk",
                      "brand": "Brand",
                      "barcode": "4601234567890",
                      "category": "DAIRY",
                      "quantity": 1.0,
                      "quantityUnit": "PIECES",
                      "expirationDate": null,
                      "expirationStatus": "FRESH",
                      "householdId": "household-id",
                      "addedByUserId": "user-id",
                      "createdAt": "2026-05-14T00:00:00Z"
                    }
                """.trimIndent(),
                status = HttpStatusCode.Created
            )
        }
        val localDataSource = FakeBarcodeLocalDataSource()

        val result = repository(engine, localDataSource)
            .addBarcodeProduct("household-id", "4601234567890")

        val added = assertIs<BarcodeAddProductResult.ProductAdded>(result)
        assertEquals("product-id", added.product.id)
        assertEquals("Milk", localDataSource.saved.single().name)
    }

    private fun repository(
        engine: MockEngine,
        localDataSource: BarcodeLocalDataSource = FakeBarcodeLocalDataSource()
    ): BarcodeRepositoryImpl =
        BarcodeRepositoryImpl(
            remoteDataSource = BarcodeRemoteDataSource(httpClient(engine)),
            localDataSource = localDataSource
        )

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }

    private fun MockRequestHandleScope.respondJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    private class FakeBarcodeLocalDataSource : BarcodeLocalDataSource {
        val saved = mutableListOf<CachedBarcodeProduct>()
        private val cached = mutableMapOf<String, CachedBarcodeProduct>()

        override suspend fun getCachedBarcode(code: String): CachedBarcodeProduct? = cached[code]

        override suspend fun saveBarcode(product: CachedBarcodeProduct) {
            saved += product
            cached[product.barcode] = product
        }

        override suspend fun isBarcodeKnown(code: String): Boolean = cached.containsKey(code)
    }
}
