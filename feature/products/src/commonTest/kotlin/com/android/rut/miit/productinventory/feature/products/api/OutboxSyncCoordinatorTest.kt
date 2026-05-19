package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OutboxSyncCoordinatorTest {

    @Test
    fun `sync serializes concurrent requests and reruns once`() = runTest {
        val productRepository = FakeProductRepository(delayMillis = 50)
        val categoryRepository = FakeCategoryRepository()
        val coordinator = OutboxSyncCoordinator(productRepository, categoryRepository)

        val first = async { coordinator.sync("household-id") }
        delay(10)
        val second = async { coordinator.sync("household-id") }

        first.await()
        second.await()

        assertEquals(2, productRepository.refreshCount)
        assertEquals(1, productRepository.maxConcurrent)
    }

    @Test
    fun `sync keeps households independent`() = runTest {
        val productRepository = FakeProductRepository()
        val categoryRepository = FakeCategoryRepository()
        val coordinator = OutboxSyncCoordinator(productRepository, categoryRepository)

        coordinator.sync("household-1")
        coordinator.sync("household-2")

        assertEquals(listOf("household-1", "household-2"), productRepository.householdIds)
    }

    @Test
    fun `sync surfaces failure after releasing running state`() = runTest {
        val productRepository = FakeProductRepository(shouldFail = true)
        val categoryRepository = FakeCategoryRepository()
        val coordinator = OutboxSyncCoordinator(productRepository, categoryRepository)

        assertFailsWith<IllegalStateException> { coordinator.sync("household-id") }
        productRepository.shouldFail = false
        coordinator.sync("household-id")

        assertEquals(2, productRepository.refreshCount)
    }

    private class FakeProductRepository(
        private val delayMillis: Long = 0,
        var shouldFail: Boolean = false
    ) : ProductRepository {
        var refreshCount = 0
            private set
        var maxConcurrent = 0
            private set
        val householdIds = mutableListOf<String>()
        private var concurrent = 0

        override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> = emptyList()

        override suspend fun refreshProducts(householdId: String, categoryId: String?): List<Product> {
            refreshCount += 1
            householdIds += householdId
            concurrent += 1
            maxConcurrent = maxOf(maxConcurrent, concurrent)
            delay(delayMillis)
            concurrent -= 1
            if (shouldFail) error("sync failed")
            return emptyList()
        }

        override suspend fun getProduct(householdId: String, productId: String): Product =
            error("not used")

        override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product =
            error("not used")

        override suspend fun deleteProduct(householdId: String, productId: String) = Unit

        override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()

        override suspend fun suggestProductEnrichment(
            householdId: String,
            name: String?,
            brand: String?,
            barcode: String?,
            ingredientsText: String?
        ): ProductEnrichmentSuggestion = error("not used")

        override suspend fun upsertCachedProduct(product: Product) = Unit

        override suspend fun deleteCachedProduct(productId: String) = Unit

        override suspend fun addProduct(
            householdId: String,
            name: String,
            category: ProductCategory,
            categoryId: String?,
            quantity: Double,
            quantityUnit: QuantityUnit,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            imageUrl: String?,
            localImagePath: String?,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product = error("not used")

        override suspend fun updateProduct(
            householdId: String,
            productId: String,
            name: String?,
            category: ProductCategory?,
            categoryId: String?,
            quantity: Double?,
            quantityUnit: QuantityUnit?,
            expirationDate: LocalDate?,
            brand: String?,
            barcode: String?,
            packageAmount: Double?,
            packageUnit: QuantityUnit?,
            ingredientsText: String?,
            imageUrl: String?,
            localImagePath: String?,
            clearImage: Boolean,
            calories: Double?,
            protein: Double?,
            fat: Double?,
            carbs: Double?,
            purchaseDate: LocalDate?,
            remainingAmount: Double?,
            lowStockThreshold: Double?
        ): Product = error("not used")
    }

    private class FakeCategoryRepository : CategoryRepository {
        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            emptyList()

        override suspend fun refreshCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            emptyList()

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
            error("not used")

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
            error("not used")

        override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
    }
}
