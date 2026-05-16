package com.android.rut.miit.productinventory.core.local

import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class PersistentLocalDataSourceTest {

    @Test
    fun productDataSourceRestoresProductsFromPersistentStore() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val original = PersistentProductLocalDataSource(store)
        original.saveProduct(product(id = "p1", barcode = "4607000000012"))

        val restored = PersistentProductLocalDataSource(store)

        assertEquals(listOf(product(id = "p1", barcode = "4607000000012")), restored.getProducts("h1"))
        assertEquals("p1", restored.getProduct("h1", "p1")?.id)
        assertEquals("p1", restored.getProductByBarcode("4607000000012")?.id)
    }

    @Test
    fun productDataSourceDeletesProductAcrossAllHouseholds() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val dataSource = PersistentProductLocalDataSource(store)
        dataSource.saveProduct(product(id = "p1", householdId = "h1"))
        dataSource.saveProduct(product(id = "p2", householdId = "h2"))

        dataSource.deleteProduct("p1")

        assertTrue(dataSource.getProducts("h1").isEmpty())
        assertEquals(listOf(product(id = "p2", householdId = "h2")), dataSource.getProducts("h2"))
    }

    @Test
    fun householdDataSourceRestoresHouseholdsFromPersistentStore() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val original = PersistentHouseholdLocalDataSource(store)
        original.saveHouseholds(
            listOf(com.android.rut.miit.productinventory.feature.household.api.models.Household("h1", "Home", "2026-05-14"))
        )

        val restored = PersistentHouseholdLocalDataSource(store)

        assertEquals("Home", restored.getHouseholdById("h1")?.name)
        assertNull(restored.getHouseholdById("missing"))
    }

    @Test
    fun barcodeDataSourceRestoresCachedBarcodesFromPersistentStore() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val original = PersistentBarcodeLocalDataSource(store)
        original.saveBarcode(CachedBarcodeProduct("4607000000012", "Milk", "DAIRY", null))

        val restored = PersistentBarcodeLocalDataSource(store)

        assertTrue(restored.isBarcodeKnown("4607000000012"))
        assertEquals("Milk", restored.getCachedBarcode("4607000000012")?.name)
        assertFalse(restored.isBarcodeKnown("missing"))
    }

    @Test
    fun syncQueueRestoresAndMutatesPendingActionsFromPersistentStore() = runTest {
        val store = InMemoryPersistentKeyValueStore()
        val original = PersistentSyncQueue(store)
        original.addPendingAction(PendingSyncAction("a1", SyncActionType.ADD_PRODUCT, "p1", "h1", "{}", 1L))
        original.addPendingAction(PendingSyncAction("a2", SyncActionType.DELETE_PRODUCT, "p2", "h1", "{}", 2L))

        val restored = PersistentSyncQueue(store)

        assertEquals(listOf("a1", "a2"), restored.getPendingActions().map { it.id })
        restored.removePendingAction("a1")
        assertEquals(listOf("a2"), PersistentSyncQueue(store).getPendingActions().map { it.id })
        restored.clearAll()
        assertTrue(PersistentSyncQueue(store).getPendingActions().isEmpty())
    }

    private fun product(
        id: String,
        householdId: String = "h1",
        barcode: String? = null
    ) = Product(
        id = id,
        name = "Milk",
        brand = "Brand",
        barcode = barcode,
        category = ProductCategory.DAIRY,
        quantity = 1.0,
        quantityUnit = QuantityUnit.PIECES,
        packageAmount = 1.0,
        packageUnit = QuantityUnit.PIECES,
        ingredientsText = "milk",
        calories = 42.0,
        protein = 3.0,
        fat = 2.0,
        carbs = 5.0,
        purchaseDate = LocalDate.parse("2026-05-14"),
        remainingAmount = 1.0,
        lowStockThreshold = 0.5,
        expirationDate = LocalDate.parse("2026-05-20"),
        expirationStatus = ExpirationStatus.FRESH,
        householdId = householdId,
        addedByUserId = "u1",
        createdAt = "2026-05-14T00:00:00Z"
    )
}

private class InMemoryPersistentKeyValueStore : PersistentKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override suspend fun read(key: String): String? = values[key]

    override suspend fun write(key: String, value: String) {
        values[key] = value
    }

    override suspend fun remove(key: String) {
        values.remove(key)
    }
}
