package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.barcode.cache

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.barcode.BarcodeLookupContext
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocalDatabaseBarcodeProductProviderTest {

    private val userId = UUID.randomUUID()
    private val householdId = UUID.randomUUID()
    private val otherHouseholdId = UUID.randomUUID()

    @Test
    fun `maps existing local product from matching household to barcode draft`() {
        val product = product(householdId = householdId)
        val provider = LocalDatabaseBarcodeProductProvider(FakeProductRepository(listOf(product)))

        val draft = provider.findDraft(context(householdId = householdId, barcode = "4601234567890"))

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
    fun `returns null when local product does not exist in requested household`() {
        val provider = LocalDatabaseBarcodeProductProvider(
            FakeProductRepository(listOf(product(householdId = otherHouseholdId)))
        )

        assertNull(provider.findDraft(context(householdId = householdId, barcode = "4601234567890")))
    }

    @Test
    fun `does not return same barcode from another household`() {
        val provider = LocalDatabaseBarcodeProductProvider(
            FakeProductRepository(
                listOf(
                    product(name = "Other Household Milk", householdId = otherHouseholdId)
                )
            )
        )

        assertNull(provider.findDraft(context(householdId = householdId, barcode = "4601234567890")))
    }

    private fun context(householdId: UUID, barcode: String): BarcodeLookupContext =
        BarcodeLookupContext(
            userId = userId,
            householdId = householdId,
            barcode = barcode
        )

    private fun product(
        name: String = "Milk",
        householdId: UUID
    ): Product =
        Product(
            name = name,
            barcode = "4601234567890",
            brand = "Brand",
            category = ProductCategory.DAIRY,
            quantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
            ingredientsText = "milk",
            calories = 60.0,
            protein = 3.2,
            fat = 3.5,
            carbs = 4.7,
            householdId = householdId,
            addedByUserId = UUID.randomUUID()
        )
}

private class FakeProductRepository(
    private val products: List<Product>
) : IProductRepository {
    override fun findById(id: UUID): Product? = null

    override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
        products.firstOrNull { it.barcode == barcode && it.householdId == householdId }

    override fun findByHouseholdId(householdId: UUID): List<Product> = emptyList()
    override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> = emptyList()
    override fun save(product: Product): Product = product
    override fun deleteById(id: UUID) = Unit
    override fun existsById(id: UUID): Boolean = false
}
