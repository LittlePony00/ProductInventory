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
            category = ProductCategory.DAIRY,
            quantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
            ingredientsText = "milk",
            calories = 60.0,
            protein = 3.2,
            fat = 3.5,
            carbs = 4.7,
            householdId = UUID.randomUUID(),
            addedByUserId = UUID.randomUUID()
        )
        val provider = LocalDatabaseBarcodeProductProvider(FakeProductRepository(product))

        val draft = provider.findDraft("4601234567890")

        assertEquals("4601234567890", draft?.barcode)
        assertEquals("Milk", draft?.name)
        assertEquals("Brand", draft?.brand)
        assertEquals("milk", draft?.ingredients)
        assertEquals(1000.0, draft?.packageQuantity?.value)
        assertEquals(QuantityUnit.MILLILITERS, draft?.packageQuantity?.unit)
        assertEquals(60.0, draft?.nutrition?.caloriesKcal)
        assertEquals(3.2, draft?.nutrition?.proteinGrams)
        assertEquals(3.5, draft?.nutrition?.fatGrams)
        assertEquals(4.7, draft?.nutrition?.carbohydratesGrams)
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
