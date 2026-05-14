package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalDatabaseBarcodeProductProviderTest {

    @Test
    fun `maps existing local product to barcode draft`() {
        val product = Product(
            name = "Milk",
            barcode = "4601234567890",
            brand = "Brand",
            ingredients = "milk",
            caloriesKcal = 60.0,
            proteinGrams = 3.2,
            fatGrams = 3.5,
            carbohydratesGrams = 4.7,
            category = ProductCategory.DAIRY,
            quantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
        val provider = LocalDatabaseBarcodeProductProvider(FakeProductRepository(product))

        val draft = provider.findDraft("4601234567890")

        assertEquals("Milk", draft?.name)
        assertEquals("Brand", draft?.brand)
        assertEquals(QuantityUnit.MILLILITERS, draft?.packageQuantity?.unit)
        assertEquals(60.0, draft?.nutrition?.caloriesKcal)
        assertEquals(ProductCategory.DAIRY, draft?.category)
        assertEquals(BarcodeProductSource.LOCAL_DATABASE, draft?.source)
    }

    @Test
    fun `returns null when local product does not exist`() {
        val provider = LocalDatabaseBarcodeProductProvider(FakeProductRepository(null))

        assertNull(provider.findDraft("missing"))
    }
}

private class FakeProductRepository(
    private val product: Product?
) : IProductRepository {
    override fun findById(id: UUID): Product? = null
    override fun findFirstByBarcode(barcode: String): Product? = product
    override fun findByHouseholdId(householdId: UUID): List<Product> = emptyList()
    override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()
    override fun save(product: Product): Product = product
    override fun deleteById(id: UUID) = Unit
    override fun existsById(id: UUID): Boolean = false
}
