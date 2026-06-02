package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.local.BarcodeLocalDataSource
import com.android.rut.miit.productinventory.core.local.CachedBarcodeProduct
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.core.network.ApiException
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUploadProductImagePayloadDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ProductRepositoryImplTest {

    @Test
    fun `consume product posts amount and caches updated product`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/households/household-id/products/product-id/consume", request.url.encodedPath)
            assertEquals("""{"amount":0.75}""", request.body.bodyText())
            respondProductJson(
                content = """
                    {
                      "id": "product-id",
                      "name": "Milk",
                      "category": "DAIRY",
                      "quantity": 2.0,
                      "quantityUnit": "PIECES",
                      "remainingAmount": 1.25,
                      "expirationDate": null,
                      "expirationStatus": "FRESH",
                      "householdId": "household-id",
                      "addedByUserId": "user-id",
                      "createdAt": "2026-05-14T00:00:00Z"
                    }
                """.trimIndent()
            )
        }

        val product = repository(engine, localDataSource)
            .consumeProduct("household-id", "product-id", 0.75)

        assertEquals(1.25, product.remainingAmount)
        assertEquals("product-id", localDataSource.saved.single().id)
        assertEquals(1.25, localDataSource.saved.single().remainingAmount)
    }

    @Test
    fun `get product falls back to cached product when remote fails`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        localDataSource.saveProduct(product(id = "product-id", name = "Cached milk"))
        val engine = MockEngine { error("offline") }

        val product = repository(engine, localDataSource).getProduct("household-id", "product-id")

        assertEquals("Cached milk", product.name)
    }

    @Test
    fun `get products returns cached products before touching remote`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        localDataSource.saveProduct(product(id = "product-id", name = "Cached milk"))
        var remoteRequests = 0
        val engine = MockEngine {
            remoteRequests += 1
            respondProductJson("[${productJson(id = "product-id", name = "Remote milk")}]")
        }

        val products = repository(engine, localDataSource).getProducts("household-id")

        assertEquals(listOf("Cached milk"), products.map { it.name })
        assertEquals(0, remoteRequests)
    }

    @Test
    fun `get products returns empty local cache without touching remote`() = runTest {
        var remoteRequests = 0
        val engine = MockEngine {
            remoteRequests += 1
            error("remote should not be called")
        }

        val products = repository(engine).getProducts("household-id")

        assertEquals(emptyList(), products)
        assertEquals(0, remoteRequests)
    }

    @Test
    fun `offline add saves local product and queues add action`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val engine = MockEngine { error("offline") }

        val product = repository(engine, localDataSource, syncQueue).addProduct(
            householdId = "household-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            imageUrl = "https://cdn.example.test/milk.jpg",
            localImagePath = "/local/product-images/milk.jpg"
        )

        assertEquals("Offline milk", localDataSource.getProduct("household-id", product.id)?.name)
        assertEquals("https://cdn.example.test/milk.jpg", localDataSource.getProduct("household-id", product.id)?.imageUrl)
        assertEquals("/local/product-images/milk.jpg", localDataSource.getProduct("household-id", product.id)?.localImagePath)
        assertEquals(
            listOf(SyncActionType.ADD_PRODUCT, SyncActionType.UPLOAD_PRODUCT_IMAGE),
            syncQueue.actions.map { it.type }
        )
        assertEquals(listOf(product.id, product.id), syncQueue.actions.map { it.entityId })
        assertTrue(syncQueue.actions.first().payload.contains("\"localImagePath\"").not())
        assertTrue(syncQueue.actions.last().payload.contains("\"localImagePath\":\"/local/product-images/milk.jpg\""))
    }

    @Test
    fun `add product with barcode and local image saves barcode cache locally`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val barcodeLocalDataSource = FakeBarcodeLocalDataSource()
        val engine = MockEngine { error("offline") }

        repository(
            engine = engine,
            localDataSource = localDataSource,
            barcodeLocalDataSource = barcodeLocalDataSource
        ).addProduct(
            householdId = "household-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            barcode = "4601234567890",
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg"
        )

        val cached = barcodeLocalDataSource.saved.single()
        assertEquals("household-id", cached.householdId)
        assertEquals("4601234567890", cached.barcode)
        assertEquals("Offline milk", cached.name)
        assertEquals("/local/product-images/milk.jpg", cached.localImagePath)
    }

    @Test
    fun `online add with local image uploads immediately and stores remote url`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products" ->
                    respondProductJson(productJson(id = "server-product-id", name = "Milk"))
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/server-product-id/image" -> {
                    assertMultipartImageRequest(request.body)
                    respondProductJson(
                        productJson(
                            id = "server-product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/server-product-id.jpg"
                        )
                    )
                }
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val product = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = FakeProductImageFileReader()
        ).addProduct(
            householdId = "household-id",
            name = "Milk",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg"
        )

        assertEquals(
            listOf(
                "/api/v1/households/household-id/products",
                "/api/v1/households/household-id/products/server-product-id/image"
            ),
            requestedPaths
        )
        assertEquals("server-product-id", product.id)
        assertEquals("https://cdn.example.test/product-images/server-product-id.jpg", product.imageUrl)
        assertEquals("/local/product-images/milk.jpg", product.localImagePath)
        assertEquals("https://cdn.example.test/product-images/server-product-id.jpg", localDataSource.getProduct("household-id", product.id)?.imageUrl)
        assertEquals("/local/product-images/milk.jpg", localDataSource.getProduct("household-id", product.id)?.localImagePath)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `online add preserves local image and queues upload when image file cannot be read`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products" ->
                    respondProductJson(productJson(id = "server-product-id", name = "Milk"))
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val product = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = MissingProductImageFileReader
        ).addProduct(
            householdId = "household-id",
            name = "Milk",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg"
        )

        assertEquals(listOf("/api/v1/households/household-id/products"), requestedPaths)
        assertEquals("server-product-id", product.id)
        assertEquals("/local/product-images/milk.jpg", product.localImagePath)
        assertEquals("/local/product-images/milk.jpg", localDataSource.getProduct("household-id", product.id)?.localImagePath)
        assertEquals(listOf(SyncActionType.UPLOAD_PRODUCT_IMAGE), syncQueue.actions.map { it.type })
        assertEquals("server-product-id", syncQueue.actions.single().entityId)
    }

    @Test
    fun `online add falls back to json image upload when multipart upload is rejected`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products" ->
                    respondProductJson(productJson(id = "server-product-id", name = "Milk"))
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/server-product-id/image" ->
                    error("multipart rejected")
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/server-product-id/image-bytes" -> {
                    assertEquals(
                        """{"fileName":"milk.jpg","contentType":"image/jpeg","bytesBase64":"AQID"}""",
                        request.body.bodyText()
                    )
                    respondProductJson(
                        productJson(
                            id = "server-product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/server-product-id.jpg"
                        )
                    )
                }
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val product = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = FakeProductImageFileReader()
        ).addProduct(
            householdId = "household-id",
            name = "Milk",
            category = ProductCategory.DAIRY,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg"
        )

        assertEquals(
            listOf(
                "/api/v1/households/household-id/products",
                "/api/v1/households/household-id/products/server-product-id/image",
                "/api/v1/households/household-id/products/server-product-id/image-bytes"
            ),
            requestedPaths
        )
        assertEquals("https://cdn.example.test/product-images/server-product-id.jpg", product.imageUrl)
        assertEquals("/local/product-images/milk.jpg", product.localImagePath)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `offline update saves local product and queues update action`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val engine = MockEngine { error("offline") }

        val updated = repository(engine, localDataSource, syncQueue).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            remainingAmount = 1.5
        )

        assertEquals("Offline milk", updated.name)
        assertEquals(1.5, localDataSource.getProduct("household-id", "product-id")?.remainingAmount)
        assertEquals(listOf(SyncActionType.UPDATE_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `offline update with local image queues metadata update and image upload separately`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val engine = MockEngine { error("offline") }

        val updated = repository(engine, localDataSource, syncQueue).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Offline milk with photo",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg",
            clearImage = false
        )

        assertEquals("/local/product-images/milk.jpg", updated.localImagePath)
        assertEquals(
            listOf(SyncActionType.UPDATE_PRODUCT, SyncActionType.UPLOAD_PRODUCT_IMAGE),
            syncQueue.actions.map { it.type }
        )
        assertTrue(syncQueue.actions.first().payload.contains("\"localImagePath\"").not())
        assertTrue(syncQueue.actions.last().payload.contains("\"localImagePath\":\"/local/product-images/milk.jpg\""))
    }

    @Test
    fun `online update with local image uploads immediately and stores remote url`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Put &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/product-id" ->
                    respondProductJson(productJson(id = "product-id", name = "Milk with photo"))
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/product-id/image" -> {
                    assertMultipartImageRequest(request.body)
                    respondProductJson(
                        productJson(
                            id = "product-id",
                            name = "Milk with photo",
                            imageUrl = "https://cdn.example.test/product-images/product-id.jpg"
                        )
                    )
                }
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val updated = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = FakeProductImageFileReader()
        ).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Milk with photo",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg",
            clearImage = false
        )

        assertEquals(
            listOf(
                "/api/v1/households/household-id/products/product-id",
                "/api/v1/households/household-id/products/product-id/image"
            ),
            requestedPaths
        )
        assertEquals("https://cdn.example.test/product-images/product-id.jpg", updated.imageUrl)
        assertEquals("/local/product-images/milk.jpg", updated.localImagePath)
        assertEquals("https://cdn.example.test/product-images/product-id.jpg", localDataSource.getProduct("household-id", "product-id")?.imageUrl)
        assertEquals("/local/product-images/milk.jpg", localDataSource.getProduct("household-id", "product-id")?.localImagePath)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `refresh products replays queued update and clears action when backend returns`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val offlineEngine = MockEngine { error("offline") }
        repository(offlineEngine, localDataSource, syncQueue).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Synced milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            remainingAmount = 1.5
        )

        val requestedPaths = mutableListOf<String>()
        val onlineEngine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when (request.method) {
                HttpMethod.Put -> respondProductJson(productJson(name = "Synced milk", remainingAmount = 1.5))
                HttpMethod.Get -> respondProductJson("[${productJson(name = "Synced milk", remainingAmount = 1.5)}]")
                else -> error("Unexpected request ${request.method}")
            }
        }

        val products = repository(onlineEngine, localDataSource, syncQueue).refreshProducts("household-id")

        assertEquals(
            listOf(
                "/api/v1/households/household-id/products/product-id",
                "/api/v1/households/household-id/products"
            ),
            requestedPaths
        )
        assertEquals("Synced milk", products.single().name)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `refresh products replays queued image upload and caches remote image`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val imageLocalCache = FakeProductImageLocalCache()
        localDataSource.saveProduct(product(id = "product-id", localImagePath = "/local/product-images/milk.jpg"))
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-image",
                type = SyncActionType.UPLOAD_PRODUCT_IMAGE,
                entityId = "product-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingUploadProductImagePayloadDto(
                        localImagePath = "/local/product-images/milk.jpg"
                    )
                ),
                createdAt = 1L
            )
        )
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/product-id/image" ->
                    respondProductJson(
                        productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/product-id.jpg"
                        )
                    )
                request.method == HttpMethod.Get &&
                    request.url.host == "cdn.example.test" ->
                    respond(byteArrayOf(9, 8, 7))
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/product-id.jpg"
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = FakeProductImageFileReader(),
            imageLocalCache = imageLocalCache
        ).refreshProducts("household-id")

        val expectedCachedPath = imageLocalCache.localPathForRemoteImage(
            productId = "product-id",
            imageUrl = "https://cdn.example.test/product-images/product-id.jpg"
        )
        assertEquals(
            listOf(
                "/api/v1/households/household-id/products/product-id/image",
                "/api/v1/households/household-id/products",
                "/product-images/product-id.jpg"
            ),
            requestedPaths
        )
        assertTrue(syncQueue.actions.isEmpty())
        assertEquals("https://cdn.example.test/product-images/product-id.jpg", products.single().imageUrl)
        assertEquals(expectedCachedPath, products.single().localImagePath)
        assertEquals(expectedCachedPath, localDataSource.getProduct("household-id", "product-id")?.localImagePath)
        assertEquals(byteArrayOf(9, 8, 7).toList(), imageLocalCache.writes.getValue(expectedCachedPath).toList())
    }

    @Test
    fun `refresh products downloads remote image and replaces stale local path`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val imageLocalCache = FakeProductImageLocalCache()
        localDataSource.saveProduct(
            product(
                id = "product-id",
                imageUrl = "https://cdn.example.test/product-images/old.jpg",
                localImagePath = "/local/stale-product.jpg"
            )
        )
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Get &&
                    request.url.host == "cdn.example.test" ->
                    respond(
                        content = byteArrayOf(9, 8, 7),
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Image.JPEG.toString())
                    )
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/new.jpg"
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource,
            imageLocalCache = imageLocalCache
        ).refreshProducts("household-id")

        val expectedCachedPath = imageLocalCache.localPathForRemoteImage(
            productId = "product-id",
            imageUrl = "https://cdn.example.test/product-images/new.jpg"
        )
        assertEquals(
            listOf(
                "/api/v1/households/household-id/products",
                "/product-images/new.jpg"
            ),
            requestedPaths
        )
        assertEquals(expectedCachedPath, products.single().localImagePath)
        assertEquals(expectedCachedPath, localDataSource.getProduct("household-id", "product-id")?.localImagePath)
        assertEquals(byteArrayOf(9, 8, 7).toList(), imageLocalCache.writes.getValue(expectedCachedPath).toList())
    }

    @Test
    fun `refresh products uses remote url instead of stale local path when remote cache write fails`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val imageLocalCache = FakeProductImageLocalCache(writeSucceeds = false)
        localDataSource.saveProduct(
            product(
                id = "product-id",
                imageUrl = "https://cdn.example.test/product-images/old.jpg",
                localImagePath = "/local/stale-product.jpg"
            )
        )
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get &&
                    request.url.host == "cdn.example.test" ->
                    respond(byteArrayOf(9, 8, 7))
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/new.jpg"
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource,
            imageLocalCache = imageLocalCache
        ).refreshProducts("household-id")

        assertEquals("https://cdn.example.test/product-images/new.jpg", products.single().imageUrl)
        assertNull(products.single().localImagePath)
        assertNull(localDataSource.getProduct("household-id", "product-id")?.localImagePath)
    }

    @Test
    fun `refresh products does not preserve missing cached path when remote cache rewrite fails`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val imageLocalCache = FakeProductImageLocalCache(writeSucceeds = false)
        val imageUrl = "https://cdn.example.test/product-images/product-id.jpg"
        val missingCachedPath = imageLocalCache.localPathForRemoteImage("product-id", imageUrl)
        localDataSource.saveProduct(
            product(
                id = "product-id",
                imageUrl = imageUrl,
                localImagePath = missingCachedPath
            )
        )
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get &&
                    request.url.host == "cdn.example.test" ->
                    respond(byteArrayOf(9, 8, 7))
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = imageUrl
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource,
            imageLocalCache = imageLocalCache
        ).refreshProducts("household-id")

        assertEquals(imageUrl, products.single().imageUrl)
        assertNull(products.single().localImagePath)
        assertNull(localDataSource.getProduct("household-id", "product-id")?.localImagePath)
    }

    @Test
    fun `refresh products uses remote url when remote image cache is unavailable`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        localDataSource.saveProduct(
            product(
                id = "product-id",
                imageUrl = "https://cdn.example.test/product-images/old.jpg",
                localImagePath = "/local/stale-product.jpg"
            )
        )
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "product-id",
                            name = "Milk",
                            imageUrl = "https://cdn.example.test/product-images/new.jpg"
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource
        ).refreshProducts("household-id")

        assertEquals("https://cdn.example.test/product-images/new.jpg", products.single().imageUrl)
        assertNull(products.single().localImagePath)
        assertNull(localDataSource.getProduct("household-id", "product-id")?.localImagePath)
    }

    @Test
    fun `upsert cached product preserves pending local image state`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val barcodeLocalDataSource = FakeBarcodeLocalDataSource()
        localDataSource.saveProduct(
            product(
                id = "product-id",
                imageUrl = null,
                localImagePath = "/local/product-images/milk.jpg"
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-image",
                type = SyncActionType.UPLOAD_PRODUCT_IMAGE,
                entityId = "product-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingUploadProductImagePayloadDto(
                        localImagePath = "/local/product-images/milk.jpg"
                    )
                ),
                createdAt = 1L
            )
        )
        val engine = MockEngine { error("Remote image should not be downloaded while a local upload is pending") }

        val cached = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            barcodeLocalDataSource = barcodeLocalDataSource,
            imageLocalCache = FakeProductImageLocalCache()
        ).upsertCachedProduct(
            product(
                id = "product-id",
                imageUrl = "https://cdn.example.test/product-images/product-id.jpg",
                localImagePath = null
            )
        )

        assertEquals("/local/product-images/milk.jpg", cached.localImagePath)
        assertNull(cached.imageUrl)
        assertEquals("/local/product-images/milk.jpg", localDataSource.getProduct("household-id", "product-id")?.localImagePath)
        assertNull(localDataSource.getProduct("household-id", "product-id")?.imageUrl)
        assertEquals("4601234567890", barcodeLocalDataSource.saved.single().barcode)
        assertEquals("/local/product-images/milk.jpg", barcodeLocalDataSource.saved.single().localImagePath)
    }

    @Test
    fun `refresh products clears orphaned pending update and image upload when local product is missing`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-update",
                type = SyncActionType.UPDATE_PRODUCT,
                entityId = "orphan-product-id",
                householdId = "household-id",
                payload = """
                    {
                      "request": {
                        "name": "Orphan milk",
                        "category": "DAIRY",
                        "quantity": 1.0,
                        "quantityUnit": "PIECES"
                      }
                    }
                """.trimIndent(),
                createdAt = 1L
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-upload",
                type = SyncActionType.UPLOAD_PRODUCT_IMAGE,
                entityId = "orphan-product-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingUploadProductImagePayloadDto(
                        localImagePath = "/local/product-images/milk.jpg"
                    )
                ),
                createdAt = 2L
            )
        )
        val engine = MockEngine { request ->
            when {
                request.method == HttpMethod.Put -> throw ApiException(404, "Product not found")
                request.method == HttpMethod.Get -> respondProductJson("[]")
                else -> error("Unexpected request ${request.method} ${request.url}")
            }
        }

        val products = repository(
            engine = engine,
            localDataSource = localDataSource,
            syncQueue = syncQueue
        ).refreshProducts("household-id")

        assertTrue(products.isEmpty())
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `refresh products replays offline add then remapped image upload`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val imageLocalCache = FakeProductImageLocalCache()
        val offlineEngine = MockEngine { error("offline") }
        val localProduct = repository(offlineEngine, localDataSource, syncQueue).addProduct(
            householdId = "household-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            imageUrl = null,
            localImagePath = "/local/product-images/milk.jpg"
        )
        val requestedPaths = mutableListOf<String>()
        val onlineEngine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products" ->
                    respondProductJson(productJson(id = "server-product-id", name = "Offline milk"))
                request.method == HttpMethod.Post &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/server-product-id/image" ->
                    respondProductJson(
                        productJson(
                            id = "server-product-id",
                            name = "Offline milk",
                            imageUrl = "https://cdn.example.test/product-images/server-product-id.jpg"
                        )
                    )
                request.method == HttpMethod.Get &&
                    request.url.host == "cdn.example.test" ->
                    respond(byteArrayOf(9, 8, 7))
                request.method == HttpMethod.Get ->
                    respondProductJson(
                        "[${productJson(
                            id = "server-product-id",
                            name = "Offline milk",
                            imageUrl = "https://cdn.example.test/product-images/server-product-id.jpg"
                        )}]"
                    )
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        val products = repository(
            engine = onlineEngine,
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            imageFileReader = FakeProductImageFileReader(),
            imageLocalCache = imageLocalCache
        ).refreshProducts("household-id")

        val expectedCachedPath = imageLocalCache.localPathForRemoteImage(
            productId = "server-product-id",
            imageUrl = "https://cdn.example.test/product-images/server-product-id.jpg"
        )
        assertEquals(
            listOf(
                "/api/v1/households/household-id/products",
                "/api/v1/households/household-id/products/server-product-id/image",
                "/api/v1/households/household-id/products",
                "/product-images/server-product-id.jpg"
            ),
            requestedPaths
        )
        assertNull(localDataSource.getProduct("household-id", localProduct.id))
        assertEquals("server-product-id", products.single().id)
        assertEquals("https://cdn.example.test/product-images/server-product-id.jpg", products.single().imageUrl)
        assertEquals(expectedCachedPath, products.single().localImagePath)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `concurrent refresh products replays pending add once`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val tempProduct = product(id = "temp-product-id", name = "OfflineAdded", remainingAmount = 3.0)
        localDataSource.saveProduct(tempProduct)
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-1",
                type = SyncActionType.ADD_PRODUCT,
                entityId = tempProduct.id,
                householdId = "household-id",
                payload = """
                    {
                      "name": "OfflineAdded",
                      "category": "DAIRY",
                      "quantity": 3.0,
                      "quantityUnit": "PIECES",
                      "remainingAmount": 3.0
                    }
                """.trimIndent(),
                createdAt = 1L
            )
        )
        var addCount = 0
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Post -> {
                    addCount += 1
                    delay(50)
                    respondProductJson(productJson(id = "server-product-id", name = "OfflineAdded", remainingAmount = 3.0))
                }
                HttpMethod.Get -> respondProductJson(
                    "[${productJson(id = "server-product-id", name = "OfflineAdded", remainingAmount = 3.0)}]"
                )
                else -> error("Unexpected request ${request.method}")
            }
        }
        val repository = repository(engine, localDataSource, syncQueue)

        val first = async { repository.refreshProducts("household-id") }
        val second = async { repository.refreshProducts("household-id") }

        assertEquals("OfflineAdded", first.await().single().name)
        assertEquals("OfflineAdded", second.await().single().name)
        assertEquals(1, addCount)
        assertTrue(syncQueue.actions.isEmpty())
    }

    @Test
    fun `partial replay after add remaps queued update to server id and preserves local update`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val tempProduct = product(id = "temp-product-id", name = "OfflineAddedEdited", remainingAmount = 3.0)
        localDataSource.saveProduct(tempProduct)
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-add",
                type = SyncActionType.ADD_PRODUCT,
                entityId = tempProduct.id,
                householdId = "household-id",
                payload = """
                    {
                      "name": "OfflineAdded",
                      "category": "DAIRY",
                      "quantity": 3.0,
                      "quantityUnit": "PIECES",
                      "remainingAmount": 3.0
                    }
                """.trimIndent(),
                createdAt = 1L
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-update",
                type = SyncActionType.UPDATE_PRODUCT,
                entityId = tempProduct.id,
                householdId = "household-id",
                payload = """
                    {
                      "name": "OfflineAddedEdited",
                      "category": "DAIRY",
                      "quantity": 3.0,
                      "quantityUnit": "PIECES",
                      "remainingAmount": 3.0
                    }
                """.trimIndent(),
                createdAt = 2L
            )
        )
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Post -> respondProductJson(
                    productJson(id = "server-product-id", name = "OfflineAdded", remainingAmount = 3.0)
                )
                HttpMethod.Put -> error("offline after add")
                else -> error("offline")
            }
        }

        val products = repository(engine, localDataSource, syncQueue).refreshProducts("household-id")

        assertEquals("OfflineAddedEdited", products.single().name)
        assertNull(localDataSource.getProduct("household-id", "temp-product-id"))
        assertEquals("OfflineAddedEdited", localDataSource.getProduct("household-id", "server-product-id")?.name)
        assertEquals(listOf("server-product-id"), syncQueue.actions.map { it.entityId })
        assertEquals(listOf(SyncActionType.UPDATE_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `failed replay keeps pending action and returns cached products`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val offlineEngine = MockEngine { error("offline") }
        repository(offlineEngine, localDataSource, syncQueue).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            remainingAmount = 1.5
        )

        val products = repository(offlineEngine, localDataSource, syncQueue).refreshProducts("household-id")

        assertEquals("Offline milk", products.single().name)
        assertEquals(listOf(SyncActionType.UPDATE_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `offline consume updates local amount without going negative and queues consume action`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", remainingAmount = 0.25))
        val engine = MockEngine { error("offline") }

        val product = repository(engine, localDataSource, syncQueue)
            .consumeProduct("household-id", "product-id", 1.0)

        assertEquals(0.0, product.remainingAmount)
        assertEquals(0.0, localDataSource.getProduct("household-id", "product-id")?.remainingAmount)
        assertEquals(listOf(SyncActionType.CONSUME_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `offline delete removes local product and queues delete action`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id"))
        val engine = MockEngine { error("offline") }

        repository(engine, localDataSource, syncQueue).deleteProduct("household-id", "product-id")

        assertNull(localDataSource.getProduct("household-id", "product-id"))
        assertEquals(listOf(SyncActionType.DELETE_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `delete does not replay unrelated pending actions before removing local product`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id"))
        localDataSource.saveProduct(product(id = "other-product-id"))
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "action-update",
                type = SyncActionType.UPDATE_PRODUCT,
                entityId = "other-product-id",
                householdId = "household-id",
                payload = """{"name":"Other milk"}""",
                createdAt = 1L
            )
        )
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            requestedPaths += request.url.encodedPath
            when {
                request.method == HttpMethod.Delete &&
                    request.url.encodedPath == "/api/v1/households/household-id/products/product-id" ->
                    respondProductJson("{}", status = HttpStatusCode.NoContent)
                else -> error("Unexpected request ${request.method} ${request.url.encodedPath}")
            }
        }

        repository(engine, localDataSource, syncQueue).deleteProduct("household-id", "product-id")

        assertNull(localDataSource.getProduct("household-id", "product-id"))
        assertEquals(listOf("/api/v1/households/household-id/products/product-id"), requestedPaths)
        assertEquals(listOf("other-product-id"), syncQueue.actions.map { it.entityId })
    }

    @Test
    fun `offline delete replaces pending product mutations with delete action`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val engine = MockEngine { error("offline") }
        val repository = repository(engine, localDataSource, syncQueue)

        repository.updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            remainingAmount = 1.5
        )
        repository.deleteProduct("household-id", "product-id")

        assertNull(localDataSource.getProduct("household-id", "product-id"))
        assertEquals(listOf(SyncActionType.DELETE_PRODUCT), syncQueue.actions.map { it.type })
    }

    @Test
    fun `category filtered get products merges pending local mutations before filtering`() = runTest {
        val localDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        localDataSource.saveProduct(product(id = "product-id", name = "Milk"))
        val offlineEngine = MockEngine { error("offline") }
        repository(offlineEngine, localDataSource, syncQueue).updateProduct(
            householdId = "household-id",
            productId = "product-id",
            name = "Offline milk",
            category = ProductCategory.DAIRY,
            categoryId = "system-dairy",
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-21"),
            remainingAmount = 1.5
        )
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Put -> error("still offline for replay")
                HttpMethod.Get -> respondProductJson("[${productJson(id = "product-id", name = "Remote milk")}]")
                else -> error("Unexpected request ${request.method}")
            }
        }

        val products = repository(engine, localDataSource, syncQueue)
            .refreshProducts("household-id", "system-dairy")

        assertEquals(listOf("Offline milk"), products.map { it.name })
    }

    private fun repository(
        engine: MockEngine,
        localDataSource: ProductLocalDataSource = FakeProductLocalDataSource(),
        syncQueue: SyncQueue = FakeSyncQueue(),
        barcodeLocalDataSource: BarcodeLocalDataSource? = null,
        imageFileReader: ProductImageFileReader = NoopProductImageFileReader,
        imageLocalCache: ProductImageLocalCache = NoopProductImageLocalCache
    ): ProductRepositoryImpl =
        ProductRepositoryImpl(
            remoteDataSource = ProductRemoteDataSource(httpClient(engine)),
            localDataSource = localDataSource,
            syncQueue = syncQueue,
            barcodeLocalDataSource = barcodeLocalDataSource,
            imageFileReader = imageFileReader,
            imageLocalCache = imageLocalCache
        )

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun assertMultipartImageRequest(body: OutgoingContent) {
        val contentType = body.contentType?.toString()
        assertTrue(
            contentType?.startsWith(ContentType.MultiPart.FormData.toString()) == true,
            "Expected multipart image upload Content-Type, got $contentType"
        )
    }

    private fun MockRequestHandleScope.respondProductJson(
        content: String,
        status: HttpStatusCode = HttpStatusCode.OK
    ) = respond(
        content = content,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    )

    private fun OutgoingContent.bodyText(): String =
        when (this) {
            is TextContent -> text
            is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            else -> error("Unsupported request body type: ${this::class.simpleName}")
        }

    private fun productJson(
        id: String = "product-id",
        name: String = "Milk",
        remainingAmount: Double = 1.25,
        imageUrl: String? = null
    ): String =
        """
            {
              "id": "$id",
              "name": "$name",
              "category": "DAIRY",
              "categoryId": "system-dairy",
              "categoryName": "Dairy",
              "quantity": 2.0,
              "quantityUnit": "PIECES",
              "remainingAmount": $remainingAmount,
              "imageUrl": ${imageUrl?.let { "\"$it\"" } ?: "null"},
              "expirationDate": null,
              "expirationStatus": "FRESH",
              "householdId": "household-id",
              "addedByUserId": "user-id",
              "createdAt": "2026-05-14T00:00:00Z"
            }
        """.trimIndent()

    private fun product(
        id: String,
        name: String = "Milk",
        remainingAmount: Double = 1.0,
        imageUrl: String? = null,
        localImagePath: String? = null
    ) = Product(
        id = id,
        name = name,
        brand = "Brand",
        barcode = "4601234567890",
        category = ProductCategory.DAIRY,
        categoryId = "system-dairy",
        categoryName = "Dairy",
        quantity = 1.0,
        quantityUnit = QuantityUnit.PIECES,
        remainingAmount = remainingAmount,
        lowStockThreshold = 0.25,
        imageUrl = imageUrl,
        localImagePath = localImagePath,
        expirationDate = LocalDate.parse("2026-05-20"),
        expirationStatus = ExpirationStatus.FRESH,
        householdId = "household-id",
        addedByUserId = "user-id",
        createdAt = "2026-05-14T00:00:00Z"
    )

    private class FakeProductLocalDataSource : ProductLocalDataSource {
        val saved = mutableListOf<Product>()
        private val products = mutableMapOf<String, Product>()

        override suspend fun getProducts(householdId: String): List<Product> =
            products.values.filter { it.householdId == householdId }

        override suspend fun getProduct(householdId: String, id: String): Product? =
            products[id]?.takeIf { it.householdId == householdId }

        override suspend fun saveProducts(householdId: String, products: List<Product>) {
            this.products.entries.removeAll { it.value.householdId == householdId }
            products.forEach { saveProduct(it) }
        }

        override suspend fun getProductByBarcode(householdId: String, barcode: String): Product? =
            products.values.firstOrNull { it.householdId == householdId && it.barcode == barcode }

        override suspend fun remapCategoryId(
            householdId: String,
            oldCategoryId: String,
            newCategoryId: String,
            newCategoryName: String
        ) {
            products.replaceAll { _, product ->
                if (product.householdId == householdId && product.categoryId == oldCategoryId) {
                    product.copy(categoryId = newCategoryId, categoryName = newCategoryName)
                } else {
                    product
                }
            }
        }

        override suspend fun deleteProduct(id: String) {
            products.remove(id)
        }

        override suspend fun saveProduct(product: Product) {
            saved += product
            products[product.id] = product
        }
    }

    private class FakeSyncQueue : SyncQueue {
        val actions = mutableListOf<PendingSyncAction>()

        override suspend fun addPendingAction(action: PendingSyncAction) {
            actions.removeAll { it.id == action.id }
            actions += action
        }

        override suspend fun getPendingActions(): List<PendingSyncAction> = actions.toList()

        override suspend fun updatePendingAction(action: PendingSyncAction) {
            actions.replaceAll { if (it.id == action.id) action else it }
        }

        override suspend fun removePendingAction(id: String) {
            actions.removeAll { it.id == id }
        }

        override suspend fun clearAll() {
            actions.clear()
        }
    }

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

    private class FakeProductImageFileReader : ProductImageFileReader {
        override suspend fun read(path: String): ProductImageFileContent =
            ProductImageFileContent(
                bytes = byteArrayOf(1, 2, 3),
                fileName = "milk.jpg",
                contentType = "image/jpeg"
            )
    }

    private object MissingProductImageFileReader : ProductImageFileReader {
        override suspend fun read(path: String): ProductImageFileContent? = null
    }

    private class FakeProductImageLocalCache(
        private val writeSucceeds: Boolean = true
    ) : ProductImageLocalCache {
        val writes = mutableMapOf<String, ByteArray>()

        override fun localPathForRemoteImage(productId: String, imageUrl: String): String =
            "/cache/${remoteProductImageFileName(productId, imageUrl)}"

        override suspend fun exists(path: String): Boolean =
            writes.containsKey(path)

        override suspend fun write(path: String, bytes: ByteArray): Boolean {
            if (!writeSucceeds) return false
            writes[path] = bytes
            return true
        }
    }
}
