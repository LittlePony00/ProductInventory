package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.BarcodeNotFoundException
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductDraft
import com.android.rut.miit.productinventory.domain.model.barcode.BarcodeProductSource
import com.android.rut.miit.productinventory.domain.model.barcode.NutritionFacts
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BarcodeServiceImplTest {

    @Test
    fun `barcode add routes through product service with draft data`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productService = RecordingProductService()
        val service = service(
            barcodeProductService = StaticBarcodeProductService(
                BarcodeProductDraft(
                    barcode = "4601234567890",
                    name = " Milk ",
                    brand = "Brand",
                    packageQuantity = Quantity(1000.0, QuantityUnit.MILLILITERS),
                    ingredients = "milk",
                    nutrition = NutritionFacts(
                        caloriesKcal = 60.0,
                        proteinGrams = 3.2,
                        fatGrams = 3.5,
                        carbohydratesGrams = 4.7
                    ),
                    category = ProductCategory.DAIRY,
                    source = BarcodeProductSource.OPEN_FOOD_FACTS,
                    confidence = 0.9
                )
            ),
            productService = productService
        )

        val response = service.lookupAndAddProduct(
            householdId = householdId.toString(),
            userId = userId.toString(),
            barcode = " 4601234567890 "
        )

        val call = productService.addCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(householdId, call.householdId)
        assertEquals("Milk", call.name)
        assertEquals("Brand", call.brand)
        assertEquals("4601234567890", call.barcode)
        assertEquals(ProductCategory.DAIRY, call.category)
        assertNull(call.categoryId)
        assertEquals(1.0, call.quantity)
        assertEquals(QuantityUnit.PIECES, call.quantityUnit)
        assertEquals(1000.0, call.packageAmount)
        assertEquals(QuantityUnit.MILLILITERS, call.packageUnit)
        assertEquals("milk", call.ingredientsText)
        assertEquals(60.0, call.calories)
        assertEquals(3.2, call.protein)
        assertEquals(3.5, call.fat)
        assertEquals(4.7, call.carbs)
        assertNull(call.expirationDate)
        assertEquals("Milk", response.name)
    }

    @Test
    fun `barcode add rejects drafts without product name`() {
        val productService = RecordingProductService()
        val service = service(
            barcodeProductService = StaticBarcodeProductService(
                BarcodeProductDraft(
                    barcode = "4601234567890",
                    name = null,
                    brand = null,
                    packageQuantity = null,
                    ingredients = null,
                    nutrition = null,
                    category = null,
                    source = BarcodeProductSource.LOCAL_DATABASE,
                    confidence = 0.2
                )
            ),
            productService = productService
        )

        assertFailsWith<BarcodeNotFoundException> {
            service.lookupAndAddProduct(
                householdId = UUID.randomUUID().toString(),
                userId = UUID.randomUUID().toString(),
                barcode = "4601234567890"
            )
        }
        assertEquals(emptyList(), productService.addCalls)
    }

    private fun service(
        barcodeProductService: IBarcodeProductService,
        productService: IProductService
    ): BarcodeServiceImpl =
        BarcodeServiceImpl(
            barcodeProductService = barcodeProductService,
            productService = productService
        )
}

private class StaticBarcodeProductService(
    private val draft: BarcodeProductDraft
) : IBarcodeProductService {
    override fun getProductDraft(userId: UUID, householdId: UUID, barcode: String): BarcodeProductDraft =
        draft.copy(barcode = barcode)
}

private class RecordingProductService : IProductService {
    val addCalls = mutableListOf<AddProductCall>()

    override fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        brand: String?,
        barcode: String?,
        category: ProductCategory,
        categoryId: UUID?,
        quantity: Double,
        quantityUnit: QuantityUnit,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product {
        addCalls += AddProductCall(
            userId = userId,
            householdId = householdId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            expirationDate = expirationDate
        )
        return Product(
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = Quantity(quantity, quantityUnit),
            packageQuantity = packageAmount?.let { Quantity(it, packageUnit ?: quantityUnit) },
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            remainingAmount = remainingAmount ?: quantity,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate?.let { com.android.rut.miit.productinventory.domain.model.ExpirationDate(it) },
            householdId = householdId,
            addedByUserId = userId
        )
    }

    override fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        brand: String?,
        barcode: String?,
        category: ProductCategory?,
        categoryId: UUID?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product = error("Not used")

    override fun consumeProduct(userId: UUID, productId: UUID, amount: Double): Product = error("Not used")
    override fun deleteProduct(userId: UUID, productId: UUID) = error("Not used")
    override fun getProducts(userId: UUID, householdId: UUID, categoryId: UUID?): List<Product> = error("Not used")
    override fun getProduct(userId: UUID, productId: UUID): Product = error("Not used")
    override fun getExpiringProducts(userId: UUID, householdId: UUID, days: Int): List<Product> = error("Not used")
}

private data class AddProductCall(
    val userId: UUID,
    val householdId: UUID,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val category: ProductCategory,
    val categoryId: UUID?,
    val quantity: Double,
    val quantityUnit: QuantityUnit,
    val packageAmount: Double?,
    val packageUnit: QuantityUnit?,
    val ingredientsText: String?,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?,
    val expirationDate: LocalDate?
)
