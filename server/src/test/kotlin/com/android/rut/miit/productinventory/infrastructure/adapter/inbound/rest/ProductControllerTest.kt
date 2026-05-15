package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.CreateProductRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateProductRequest
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.util.UUID

class ProductControllerTest {

    @AfterTest
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `add product forwards extended request fields to service`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val service = RecordingProductService()
        val controller = ProductController(service)
        authenticate(userId)

        val response = controller.addProduct(
            householdId = householdId,
            request = CreateProductRequest(
                name = "Milk",
                brand = "Brand",
                barcode = "4601234567890",
                category = ProductCategory.DAIRY,
                categoryId = categoryId,
                quantity = 2.0,
                quantityUnit = QuantityUnit.PIECES,
                packageAmount = 950.0,
                packageUnit = QuantityUnit.MILLILITERS,
                ingredientsText = "Milk",
                calories = 60.0,
                protein = 3.0,
                fat = 2.5,
                carbs = 4.7,
                purchaseDate = LocalDate.of(2026, 5, 14),
                remainingAmount = 1.5,
                lowStockThreshold = 0.5,
                expirationDate = null
            )
        )

        val call = service.addCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(householdId, call.householdId)
        assertEquals(categoryId, call.categoryId)
        assertEquals("Brand", call.brand)
        assertEquals("4601234567890", call.barcode)
        assertEquals(950.0, call.packageAmount)
        assertEquals(QuantityUnit.MILLILITERS, call.packageUnit)
        assertEquals("Milk", call.ingredientsText)
        assertEquals(60.0, call.calories)
        assertEquals(3.0, call.protein)
        assertEquals(2.5, call.fat)
        assertEquals(4.7, call.carbs)
        assertEquals(LocalDate.of(2026, 5, 14), call.purchaseDate)
        assertEquals(1.5, call.remainingAmount)
        assertEquals(0.5, call.lowStockThreshold)
        assertEquals("Brand", response.brand)
        assertEquals(1.5, response.remainingAmount)
    }

    @Test
    fun `update product forwards extended request fields to service`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val service = RecordingProductService(productId = productId)
        val controller = ProductController(service)
        authenticate(userId)

        controller.updateProduct(
            householdId = householdId,
            productId = productId,
            request = UpdateProductRequest(
                name = null,
                brand = "Updated",
                barcode = "9876543210987",
                category = null,
                categoryId = categoryId,
                quantity = null,
                quantityUnit = null,
                packageAmount = 500.0,
                packageUnit = QuantityUnit.GRAMS,
                ingredientsText = "Updated ingredients",
                calories = 100.0,
                protein = 10.0,
                fat = 5.0,
                carbs = 20.0,
                purchaseDate = LocalDate.of(2026, 5, 10),
                remainingAmount = 0.25,
                lowStockThreshold = 0.1,
                expirationDate = null
            )
        )

        val call = service.updateCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(productId, call.productId)
        assertEquals(categoryId, call.categoryId)
        assertEquals("Updated", call.brand)
        assertEquals("9876543210987", call.barcode)
        assertEquals(500.0, call.packageAmount)
        assertEquals(QuantityUnit.GRAMS, call.packageUnit)
        assertEquals("Updated ingredients", call.ingredientsText)
        assertEquals(100.0, call.calories)
        assertEquals(10.0, call.protein)
        assertEquals(5.0, call.fat)
        assertEquals(20.0, call.carbs)
        assertEquals(LocalDate.of(2026, 5, 10), call.purchaseDate)
        assertEquals(0.25, call.remainingAmount)
        assertEquals(0.1, call.lowStockThreshold)
    }

    @Test
    fun `get products forwards category filter to service`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()
        val service = RecordingProductService()
        val controller = ProductController(service)
        authenticate(userId)

        controller.getProducts(householdId = householdId, categoryId = categoryId)

        val call = service.getProductsCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(householdId, call.householdId)
        assertEquals(categoryId, call.categoryId)
    }

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }

    private class RecordingProductService(
        private val productId: UUID = UUID.randomUUID()
    ) : IProductService {
        val addCalls = mutableListOf<AddCall>()
        val updateCalls = mutableListOf<UpdateCall>()
        val getProductsCalls = mutableListOf<GetProductsCall>()

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
            addCalls += AddCall(
                userId = userId,
                householdId = householdId,
                categoryId = categoryId,
                brand = brand,
                barcode = barcode,
                packageAmount = packageAmount,
                packageUnit = packageUnit,
                ingredientsText = ingredientsText,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                purchaseDate = purchaseDate,
                remainingAmount = remainingAmount,
                lowStockThreshold = lowStockThreshold
            )
            return product(
                id = productId,
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
                purchaseDate = purchaseDate,
                remainingAmount = remainingAmount ?: quantity,
                lowStockThreshold = lowStockThreshold,
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
        ): Product {
            updateCalls += UpdateCall(
                userId = userId,
                productId = productId,
                categoryId = categoryId,
                brand = brand,
                barcode = barcode,
                packageAmount = packageAmount,
                packageUnit = packageUnit,
                ingredientsText = ingredientsText,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                purchaseDate = purchaseDate,
                remainingAmount = remainingAmount,
                lowStockThreshold = lowStockThreshold
            )
            return product(
                id = productId,
                name = name ?: "Product",
                brand = brand,
                barcode = barcode,
                category = category ?: ProductCategory.OTHER,
                categoryId = categoryId,
                quantity = Quantity(quantity ?: 1.0, quantityUnit ?: QuantityUnit.PIECES),
                packageQuantity = packageAmount?.let { Quantity(it, packageUnit ?: QuantityUnit.PIECES) },
                ingredientsText = ingredientsText,
                calories = calories,
                protein = protein,
                fat = fat,
                carbs = carbs,
                purchaseDate = purchaseDate,
                remainingAmount = remainingAmount ?: 1.0,
                lowStockThreshold = lowStockThreshold,
                householdId = UUID.randomUUID(),
                addedByUserId = userId
            )
        }

        override fun deleteProduct(userId: UUID, productId: UUID) = Unit

        override fun getProducts(userId: UUID, householdId: UUID, categoryId: UUID?): List<Product> {
            getProductsCalls += GetProductsCall(userId, householdId, categoryId)
            return emptyList()
        }

        override fun getProduct(userId: UUID, productId: UUID): Product = product(
            id = productId,
            householdId = UUID.randomUUID(),
            addedByUserId = userId
        )

        override fun getExpiringProducts(userId: UUID, householdId: UUID, days: Int): List<Product> = emptyList()
    }

    private data class AddCall(
        val userId: UUID,
        val householdId: UUID,
        val categoryId: UUID?,
        val brand: String?,
        val barcode: String?,
        val packageAmount: Double?,
        val packageUnit: QuantityUnit?,
        val ingredientsText: String?,
        val calories: Double?,
        val protein: Double?,
        val fat: Double?,
        val carbs: Double?,
        val purchaseDate: LocalDate?,
        val remainingAmount: Double?,
        val lowStockThreshold: Double?
    )

    private data class UpdateCall(
        val userId: UUID,
        val productId: UUID,
        val categoryId: UUID?,
        val brand: String?,
        val barcode: String?,
        val packageAmount: Double?,
        val packageUnit: QuantityUnit?,
        val ingredientsText: String?,
        val calories: Double?,
        val protein: Double?,
        val fat: Double?,
        val carbs: Double?,
        val purchaseDate: LocalDate?,
        val remainingAmount: Double?,
        val lowStockThreshold: Double?
    )

    private data class GetProductsCall(
        val userId: UUID,
        val householdId: UUID,
        val categoryId: UUID?
    )

    private companion object {
        fun product(
            id: UUID = UUID.randomUUID(),
            name: String = "Product",
            brand: String? = null,
            barcode: String? = null,
            category: ProductCategory = ProductCategory.OTHER,
            categoryId: UUID? = null,
            quantity: Quantity = Quantity(1.0, QuantityUnit.PIECES),
            packageQuantity: Quantity? = null,
            ingredientsText: String? = null,
            calories: Double? = null,
            protein: Double? = null,
            fat: Double? = null,
            carbs: Double? = null,
            purchaseDate: LocalDate? = null,
            remainingAmount: Double = quantity.value,
            lowStockThreshold: Double? = null,
            householdId: UUID,
            addedByUserId: UUID
        ) = Product(
            id = id,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            packageQuantity = packageQuantity,
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            householdId = householdId,
            addedByUserId = addedByUserId
        )
    }
}
