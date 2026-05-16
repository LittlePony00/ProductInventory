package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.service.BarcodeServiceImpl
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
import kotlin.test.assertEquals
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class BarcodeControllerTest {

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `barcode product post resolves draft and creates product through add service`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val productService = RestRecordingProductService()
        val mockMvc = MockMvcBuilders
            .standaloneSetup(controller(productService = productService))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(userId)

        mockMvc.post("/api/v1/households/$householdId/products/barcode") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"barcode":" 4601234567890 "}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.name", equalTo("Milk"))
            jsonPath("$.brand", equalTo("Brand"))
            jsonPath("$.barcode", equalTo("4601234567890"))
            jsonPath("$.category", equalTo("DAIRY"))
            jsonPath("$.packageAmount", equalTo(1000.0))
            jsonPath("$.packageUnit", equalTo("MILLILITERS"))
        }

        val call = productService.addCalls.single()
        assertEquals(userId, call.userId)
        assertEquals(householdId, call.householdId)
        assertEquals("Milk", call.name)
        assertEquals("4601234567890", call.barcode)
        assertEquals(ProductCategory.DAIRY, call.category)
        assertEquals(1000.0, call.packageAmount)
        assertEquals(QuantityUnit.MILLILITERS, call.packageUnit)
    }

    @Test
    fun `barcode product post returns manual-entry response when draft has no name`() {
        val productService = RestRecordingProductService()
        val householdId = UUID.randomUUID()
        val mockMvc = MockMvcBuilders
            .standaloneSetup(
                controller(
                    barcodeProductService = RestStaticBarcodeProductService(
                        draft = BarcodeProductDraft(
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
            )
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
        authenticate(UUID.randomUUID())

        mockMvc.post("/api/v1/households/$householdId/products/barcode") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"barcode":"4601234567890"}"""
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.barcode", equalTo("4601234567890"))
            jsonPath("$.needsManualEntry", equalTo(true))
        }

        assertEquals(emptyList(), productService.addCalls)
    }

    private fun controller(
        barcodeProductService: IBarcodeProductService = RestStaticBarcodeProductService(
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
        productService: IProductService = RestRecordingProductService()
    ): BarcodeController =
        BarcodeController(
            BarcodeServiceImpl(
                barcodeProductService = barcodeProductService,
                productService = productService
            )
        )

    private fun authenticate(userId: UUID) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
    }
}

private class RestStaticBarcodeProductService(
    private val draft: BarcodeProductDraft
) : IBarcodeProductService {
    override fun getProductDraft(userId: UUID, householdId: UUID, barcode: String): BarcodeProductDraft =
        draft.copy(barcode = barcode)
}

private class RestRecordingProductService : IProductService {
    val addCalls = mutableListOf<RestAddProductCall>()

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
        addCalls += RestAddProductCall(
            userId = userId,
            householdId = householdId,
            name = name,
            barcode = barcode,
            category = category,
            packageAmount = packageAmount,
            packageUnit = packageUnit
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

private data class RestAddProductCall(
    val userId: UUID,
    val householdId: UUID,
    val name: String,
    val barcode: String?,
    val category: ProductCategory,
    val packageAmount: Double?,
    val packageUnit: QuantityUnit?
)
