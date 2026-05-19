package com.android.rut.miit.productinventory.feature.barcode.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeAddProductResult
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeLookupResult
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
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
import kotlinx.datetime.LocalDate
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
                      "imageUrl": "https://cdn.example.test/milk-draft.jpg",
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
        assertEquals("https://cdn.example.test/milk-draft.jpg", found.draft.imageUrl)
        assertEquals("Milk", localDataSource.saved.single().name)
        assertEquals("https://cdn.example.test/milk-draft.jpg", localDataSource.saved.single().imageUrl)
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
                      "imageUrl": "https://cdn.example.test/milk-product.jpg",
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
        assertEquals("https://cdn.example.test/milk-product.jpg", added.product.imageUrl)
        assertEquals("Milk", localDataSource.saved.single().name)
        assertEquals("https://cdn.example.test/milk-product.jpg", localDataSource.saved.single().imageUrl)
    }

    @Test
    fun `offline lookup returns cached draft with image url`() = runTest {
        val localDataSource = FakeBarcodeLocalDataSource()
        localDataSource.saveBarcode(
            CachedBarcodeProduct(
                householdId = "household-id",
                barcode = "4601234567890",
                name = "Cached milk",
                brand = null,
                category = ProductCategory.DAIRY.name,
                categoryId = null,
                categoryName = null,
                packageQuantity = null,
                packageQuantityUnit = null,
                ingredients = null,
                imageUrl = "https://cdn.example.test/cached-milk.jpg",
                localImagePath = "/local/cached-milk.jpg",
                caloriesKcal = null,
                proteinGrams = null,
                fatGrams = null,
                carbohydratesGrams = null,
                source = "LOCAL_CACHE",
                updatedAt = 0L
            )
        )
        val engine = MockEngine { error("offline") }

        val result = repository(engine, localDataSource).lookupBarcode("household-id", "4601234567890")

        val found = assertIs<BarcodeLookupResult.DraftFound>(result)
        assertEquals("Cached milk", found.draft.name)
        assertEquals(ProductCategory.DAIRY, found.draft.category)
        assertEquals("https://cdn.example.test/cached-milk.jpg", found.draft.imageUrl)
        assertEquals("/local/cached-milk.jpg", found.draft.localImagePath)
    }

    @Test
    fun `offline lookup returns local product draft before remote`() = runTest {
        val productLocalDataSource = FakeProductLocalDataSource()
        productLocalDataSource.saveProduct(product(localImagePath = "/local/product.jpg"))
        val engine = MockEngine { error("remote should not be called") }

        val result = repository(
            engine = engine,
            productLocalDataSource = productLocalDataSource
        ).lookupBarcode("household-id", "4601234567890")

        val found = assertIs<BarcodeLookupResult.DraftFound>(result)
        assertEquals("Milk", found.draft.name)
        assertEquals("/local/product.jpg", found.draft.localImagePath)
    }

    private fun repository(
        engine: MockEngine,
        localDataSource: BarcodeLocalDataSource = FakeBarcodeLocalDataSource(),
        productLocalDataSource: ProductLocalDataSource = FakeProductLocalDataSource()
    ): BarcodeRepositoryImpl =
        BarcodeRepositoryImpl(
            remoteDataSource = BarcodeRemoteDataSource(httpClient(engine)),
            localDataSource = localDataSource,
            productLocalDataSource = productLocalDataSource,
            remoteLookupTimeoutMillis = Long.MAX_VALUE
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

        override suspend fun getCachedBarcode(householdId: String, code: String): CachedBarcodeProduct? =
            cached["$householdId|${code.trim()}"]

        override suspend fun saveBarcode(product: CachedBarcodeProduct) {
            saved += product
            cached["${product.householdId}|${product.barcode.trim()}"] = product
        }

        override suspend fun isBarcodeKnown(householdId: String, code: String): Boolean =
            cached.containsKey("$householdId|${code.trim()}")
    }

    private class FakeProductLocalDataSource : ProductLocalDataSource {
        private val products = mutableListOf<Product>()

        override suspend fun getProducts(householdId: String): List<Product> =
            products.filter { it.householdId == householdId }

        override suspend fun getProduct(householdId: String, id: String): Product? =
            products.firstOrNull { it.householdId == householdId && it.id == id }

        override suspend fun saveProducts(householdId: String, products: List<Product>) {
            this.products.removeAll { it.householdId == householdId }
            this.products += products
        }

        override suspend fun getProductByBarcode(householdId: String, barcode: String): Product? =
            products.firstOrNull { it.householdId == householdId && it.barcode == barcode }

        override suspend fun remapCategoryId(
            householdId: String,
            oldCategoryId: String,
            newCategoryId: String,
            newCategoryName: String
        ) {
            products.replaceAll {
                if (it.householdId == householdId && it.categoryId == oldCategoryId) {
                    it.copy(categoryId = newCategoryId, categoryName = newCategoryName)
                } else {
                    it
                }
            }
        }

        override suspend fun deleteProduct(id: String) {
            products.removeAll { it.id == id }
        }

        override suspend fun saveProduct(product: Product) {
            products.removeAll { it.id == product.id }
            products += product
        }
    }

    private fun product(localImagePath: String? = null) = Product(
        id = "product-id",
        name = "Milk",
        brand = "Brand",
        barcode = "4601234567890",
        category = ProductCategory.DAIRY,
        quantity = 1.0,
        quantityUnit = QuantityUnit.PIECES,
        packageAmount = 950.0,
        packageUnit = QuantityUnit.MILLILITERS,
        ingredientsText = "Milk",
        imageUrl = null,
        localImagePath = localImagePath,
        expirationDate = LocalDate.parse("2026-05-20"),
        expirationStatus = ExpirationStatus.FRESH,
        householdId = "household-id",
        addedByUserId = "user-id",
        createdAt = "2026-05-14T00:00:00Z"
    )
}
