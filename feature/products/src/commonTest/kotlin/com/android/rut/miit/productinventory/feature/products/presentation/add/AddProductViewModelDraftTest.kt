package com.android.rut.miit.productinventory.feature.products.presentation.add

import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.SuggestProductEnrichmentUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSource
import com.android.rut.miit.productinventory.feature.products.api.models.ProductEnrichmentSuggestion
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddProductViewModelDraftTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun scannedDraftPrefillsFieldsAndIsSentToAddProductUseCase() = runTest {
        val repository = CapturingProductRepository()
        val viewModel = AddProductViewModel(
            AddProductUseCase(repository),
            UpdateProductUseCase(repository),
            GetProductUseCase(repository),
            GetProductCategoriesUseCase(FakeCategoryRepository()),
            CreateProductCategoryUseCase(FakeCategoryRepository()),
            SuggestProductEnrichmentUseCase(repository)
        )
        viewModel.householdId = "h1"

        viewModel.onEvent(
            AddProductEvent.OnScannedDraftApplied(
                barcode = "4601234567890",
                name = "Milk",
                brand = "Brand",
                category = ProductCategory.DAIRY,
                packageAmount = "950.0",
                packageUnit = QuantityUnit.MILLILITERS,
                ingredientsText = "Milk",
                imageUrl = "https://example.test/milk.jpg",
                calories = "60.0",
                protein = "3.0",
                fat = "2.5",
                carbs = "4.7"
            )
        )
        viewModel.onEvent(AddProductEvent.OnQuantityChanged("2"))
        viewModel.onEvent(AddProductEvent.OnExpirationDateChanged("2026-05-20"))
        viewModel.onEvent(AddProductEvent.OnSaveClick)
        advanceUntilIdle()

        val request = repository.request
        assertEquals("h1", request?.householdId)
        assertEquals("Milk", request?.name)
        assertEquals("Brand", request?.brand)
        assertEquals("4601234567890", request?.barcode)
        assertEquals(ProductCategory.DAIRY, request?.category)
        assertEquals(2.0, request?.quantity)
        assertEquals(950.0, request?.packageAmount)
        assertEquals(QuantityUnit.MILLILITERS, request?.packageUnit)
        assertEquals("Milk", request?.ingredientsText)
        assertEquals(60.0, request?.calories)
        assertEquals(3.0, request?.protein)
        assertEquals(2.5, request?.fat)
        assertEquals(4.7, request?.carbs)
    }
}

private data class CapturedAddProductRequest(
    val householdId: String,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val category: ProductCategory,
    val categoryId: String?,
    val quantity: Double,
    val packageAmount: Double?,
    val packageUnit: QuantityUnit?,
    val ingredientsText: String?,
    val imageUrl: String?,
    val calories: Double?,
    val protein: Double?,
    val fat: Double?,
    val carbs: Double?
)

private class CapturingProductRepository : ProductRepository {
    var request: CapturedAddProductRequest? = null

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
    ): Product {
        request = CapturedAddProductRequest(
            householdId = householdId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            imageUrl = imageUrl,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs
        )
        return Product(
            id = "p1",
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
            imageUrl = imageUrl,
            localImagePath = localImagePath,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            expirationDate = expirationDate,
            expirationStatus = ExpirationStatus.FRESH,
            householdId = householdId,
            addedByUserId = "u1",
            createdAt = "2026-05-14T00:00:00Z"
        )
    }

    override suspend fun getProducts(householdId: String, categoryId: String?): List<Product> = emptyList()
    override suspend fun getProduct(householdId: String, productId: String): Product = error("Unused")
    override suspend fun consumeProduct(householdId: String, productId: String, amount: Double): Product = error("Unused")
    override suspend fun deleteProduct(householdId: String, productId: String) = Unit
    override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> = emptyList()
    override suspend fun suggestProductEnrichment(
        householdId: String,
        name: String?,
        brand: String?,
        barcode: String?,
        ingredientsText: String?
    ): ProductEnrichmentSuggestion =
        ProductEnrichmentSuggestion(
            categoryId = "system-other",
            category = ProductCategory.OTHER,
            categoryName = "Другое",
            confidence = 0.2,
            source = ProductEnrichmentSource.FALLBACK,
            suggestedName = null,
            suggestedBrand = null,
            suggestedIngredientsText = null,
            calories = null,
            protein = null,
            fat = null,
            carbs = null
        )

    override suspend fun upsertCachedProduct(product: Product): Product = product
    override suspend fun deleteCachedProduct(productId: String) = Unit

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
    ): Product = error("Unused")
}

private class FakeCategoryRepository : CategoryRepository {
    override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
        ProductCategoryOption.systemDefaults()

    override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
        ProductCategoryOption(
            id = "custom-id",
            householdId = householdId,
            name = name,
            system = false,
            createdAt = "2026-05-14T00:00:00Z"
        )

    override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
        error("Unused")

    override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
}
