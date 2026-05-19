package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.core.local.CategoryLocalDataSource
import com.android.rut.miit.productinventory.core.local.PendingSyncAction
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.local.SyncActionType
import com.android.rut.miit.productinventory.core.local.SyncQueue
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.data.models.CreateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingCreateCategoryPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingCreateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUpdateCategoryPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.PendingUpdateProductPayloadDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateProductRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CategoryRepositoryImplTest {

    @Test
    fun `get categories returns system defaults and local custom categories without remote`() = runTest {
        var remoteRequests = 0
        val localDataSource = FakeCategoryLocalDataSource()
        localDataSource.saveCategory(customCategory("custom-1", "Bakery"))
        val repository = repository(
            engine = MockEngine {
                remoteRequests += 1
                error("remote should not be called")
            },
            localDataSource = localDataSource
        )

        val categories = repository.getCategories("household-id")

        assertEquals(0, remoteRequests)
        assertEquals(
            (ProductCategoryOption.systemDefaults().map { it.name } + "Bakery").sorted(),
            categories.map { it.name }.sorted()
        )
    }

    @Test
    fun `get categories returns system defaults with empty local cache and remote unavailable`() = runTest {
        val repository = repository(MockEngine { error("offline") })

        val categories = repository.getCategories("household-id")

        assertEquals(ProductCategoryOption.systemDefaults().map { it.id }.toSet(), categories.map { it.id }.toSet())
    }

    @Test
    fun `refresh categories saves remote custom categories locally`() = runTest {
        val localDataSource = FakeCategoryLocalDataSource()
        val repository = repository(
            engine = MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                respondJson(
                    """
                    [
                      {
                        "id": "custom-remote",
                        "householdId": "household-id",
                        "code": null,
                        "name": "Remote bakery",
                        "system": false,
                        "archived": false,
                        "createdAt": "2026-05-14T00:00:00Z"
                      }
                    ]
                    """.trimIndent()
                )
            },
            localDataSource = localDataSource
        )

        val categories = repository.refreshCategories("household-id")

        assertEquals("Remote bakery", categories.single { !it.system }.name)
        assertEquals("Remote bakery", localDataSource.getCategories("household-id").single().name)
    }

    @Test
    fun `create category offline saves local category and queues create action`() = runTest {
        val localDataSource = FakeCategoryLocalDataSource()
        val syncQueue = FakeSyncQueue()
        val repository = repository(
            engine = MockEngine { error("offline") },
            localDataSource = localDataSource,
            syncQueue = syncQueue
        )

        val category = repository.createCategory("household-id", "  Bakery  ")

        assertEquals("Bakery", category.name)
        assertEquals("Bakery", localDataSource.getCategories("household-id").single().name)
        assertEquals(listOf(SyncActionType.CREATE_CATEGORY), syncQueue.actions.map { it.type })
        assertEquals(category.id, syncQueue.actions.single().entityId)
    }

    @Test
    fun `refresh categories remaps created category id in local products and pending product payloads`() = runTest {
        val localDataSource = FakeCategoryLocalDataSource()
        val productLocalDataSource = FakeProductLocalDataSource()
        val syncQueue = FakeSyncQueue()
        productLocalDataSource.saveProduct(product(categoryId = "local-category-id", categoryName = "Bakery"))
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "create-category",
                type = SyncActionType.CREATE_CATEGORY,
                entityId = "local-category-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingCreateCategoryPayloadDto(
                        name = "Bakery",
                        localCategoryId = "local-category-id"
                    )
                ),
                createdAt = 1L
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "add-product",
                type = SyncActionType.ADD_PRODUCT,
                entityId = "local-product-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingCreateProductPayloadDto(
                        request = CreateProductRequestDto(
                            name = "Croissant",
                            category = ProductCategory.OTHER.name,
                            categoryId = "local-category-id",
                            quantity = 1.0,
                            quantityUnit = QuantityUnit.PIECES.name
                        )
                    )
                ),
                createdAt = 2L
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "update-product",
                type = SyncActionType.UPDATE_PRODUCT,
                entityId = "existing-product-id",
                householdId = "household-id",
                payload = Json.encodeToString(
                    PendingUpdateProductPayloadDto(
                        request = UpdateProductRequestDto(categoryId = "local-category-id")
                    )
                ),
                createdAt = 3L
            )
        )
        syncQueue.addPendingAction(
            PendingSyncAction(
                id = "update-category",
                type = SyncActionType.UPDATE_CATEGORY,
                entityId = "local-category-id",
                householdId = "household-id",
                payload = Json.encodeToString(PendingUpdateCategoryPayloadDto(name = "Fresh bakery")),
                createdAt = 4L
            )
        )
        val repository = repository(
            engine = MockEngine { request ->
                when (request.method) {
                    HttpMethod.Post -> respondJson(categoryJson(id = "remote-category-id", name = "Bakery"))
                    HttpMethod.Put -> {
                        assertEquals(
                            "/api/v1/households/household-id/categories/remote-category-id",
                            request.url.encodedPath
                        )
                        respondJson(categoryJson(id = "remote-category-id", name = "Fresh bakery"))
                    }
                    HttpMethod.Get -> respondJson("[${categoryJson(id = "remote-category-id", name = "Fresh bakery")}]")
                    else -> error("Unexpected request ${request.method}")
                }
            },
            localDataSource = localDataSource,
            productLocalDataSource = productLocalDataSource,
            syncQueue = syncQueue
        )

        repository.refreshCategories("household-id")

        val remappedProduct = productLocalDataSource.getProduct("household-id", "product-id")
        assertEquals("remote-category-id", remappedProduct?.categoryId)
        assertEquals("Bakery", remappedProduct?.categoryName)
        val productActions = syncQueue.actions.filter { it.type == SyncActionType.ADD_PRODUCT || it.type == SyncActionType.UPDATE_PRODUCT }
        assertEquals(
            "remote-category-id",
            Json.decodeFromString<PendingCreateProductPayloadDto>(productActions.first().payload).request.categoryId
        )
        assertEquals(
            "remote-category-id",
            Json.decodeFromString<PendingUpdateProductPayloadDto>(productActions.last().payload).request.categoryId
        )
    }

    private fun repository(
        engine: MockEngine,
        localDataSource: CategoryLocalDataSource = FakeCategoryLocalDataSource(),
        productLocalDataSource: ProductLocalDataSource = FakeProductLocalDataSource(),
        syncQueue: SyncQueue = FakeSyncQueue()
    ): CategoryRepositoryImpl =
        CategoryRepositoryImpl(
            remoteDataSource = CategoryRemoteDataSource(httpClient(engine)),
            localDataSource = localDataSource,
            productLocalDataSource = productLocalDataSource,
            syncQueue = syncQueue
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

    private fun MockRequestHandleScope.respondJson(content: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(
            content = content,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )

    private fun customCategory(id: String, name: String) = ProductCategoryOption(
        id = id,
        householdId = "household-id",
        name = name,
        system = false,
        createdAt = "2026-05-14T00:00:00Z"
    )

    private fun categoryJson(id: String, name: String): String =
        """
        {
          "id": "$id",
          "householdId": "household-id",
          "code": null,
          "name": "$name",
          "system": false,
          "archived": false,
          "createdAt": "2026-05-14T00:00:00Z"
        }
        """.trimIndent()

    private fun product(categoryId: String, categoryName: String) = Product(
        id = "product-id",
        name = "Croissant",
        category = ProductCategory.OTHER,
        categoryId = categoryId,
        categoryName = categoryName,
        quantity = 1.0,
        quantityUnit = QuantityUnit.PIECES,
        expirationDate = LocalDate.parse("2026-05-20"),
        expirationStatus = ExpirationStatus.FRESH,
        householdId = "household-id",
        addedByUserId = "user-id",
        createdAt = "2026-05-14T00:00:00Z"
    )

    private class FakeCategoryLocalDataSource : CategoryLocalDataSource {
        private val categories = mutableMapOf<String, List<ProductCategoryOption>>()

        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            categories[householdId].orEmpty().filter { includeArchived || !it.archived }

        override suspend fun saveCategories(householdId: String, categories: List<ProductCategoryOption>) {
            this.categories[householdId] = categories
        }

        override suspend fun saveCategory(category: ProductCategoryOption) {
            val householdId = category.householdId ?: return
            categories[householdId] = categories[householdId].orEmpty().filterNot { it.id == category.id } + category
        }

        override suspend fun archiveCategory(householdId: String, categoryId: String) {
            categories[householdId] = categories[householdId].orEmpty()
                .map { if (it.id == categoryId) it.copy(archived = true) else it }
        }

        override suspend fun deleteCategory(categoryId: String) {
            categories.replaceAll { _, values -> values.filterNot { it.id == categoryId } }
        }
    }

    private class FakeSyncQueue : SyncQueue {
        val actions = mutableListOf<PendingSyncAction>()
        override suspend fun addPendingAction(action: PendingSyncAction) { actions += action }
        override suspend fun getPendingActions(): List<PendingSyncAction> = actions.toList()
        override suspend fun updatePendingAction(action: PendingSyncAction) {
            actions.replaceAll { if (it.id == action.id) action else it }
        }
        override suspend fun removePendingAction(id: String) { actions.removeAll { it.id == id } }
        override suspend fun clearAll() { actions.clear() }
    }

    private class FakeProductLocalDataSource : ProductLocalDataSource {
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
            products[product.id] = product
        }
    }
}
