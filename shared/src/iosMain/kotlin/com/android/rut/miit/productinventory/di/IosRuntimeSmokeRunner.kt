package com.android.rut.miit.productinventory.di

import com.android.rut.miit.productinventory.feature.auth.api.RegisterUseCase
import com.android.rut.miit.productinventory.feature.household.api.CreateHouseholdUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.SuggestProductEnrichmentUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.models.HouseholdRealtimeEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

data class IosRuntimeSmokeResult(
    val email: String,
    val userId: String,
    val householdId: String,
    val productId: String,
    val productName: String,
    val productsVisible: Int,
    val categoriesVisible: Int,
    val suggestionSource: String,
    val recipeCount: Int,
    val pushEnabled: Boolean
)

data class IosRealtimeSmokeResult(
    val email: String,
    val userId: String,
    val householdId: String,
    val productId: String,
    val productName: String,
    val realtimeEventType: String,
    val realtimeEventReason: String?,
    val productsVisible: Int
)

class IosRuntimeSmokeRunner : KoinComponent {
    private val registerUseCase: RegisterUseCase = get()
    private val createHouseholdUseCase: CreateHouseholdUseCase = get()
    private val getProductCategoriesUseCase: GetProductCategoriesUseCase = get()
    private val addProductUseCase: AddProductUseCase = get()
    private val getProductsUseCase: GetProductsUseCase = get()
    private val suggestProductEnrichmentUseCase: SuggestProductEnrichmentUseCase = get()
    private val getRecipesUseCase: GetRecipesUseCase = get()
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase = get()
    private val observeHouseholdEventsUseCase: ObserveHouseholdEventsUseCase = get()

    suspend fun run(email: String, password: String, timestamp: String): IosRuntimeSmokeResult {
        val auth = registerUseCase(email = email, password = password, name = "iOS Smoke")
        val household = createHouseholdUseCase("iOS Smoke $timestamp")
        val categories = getProductCategoriesUseCase(household.id)
        val dairyCategory = categories.firstOrNull { it.legacyCategory == ProductCategory.DAIRY }
            ?: categories.first()

        val suggestion = suggestProductEnrichmentUseCase(
            householdId = household.id,
            name = "iOS Smoke Milk",
            brand = "Smoke Brand",
            barcode = null,
            ingredientsText = "milk"
        )

        val product = addProductUseCase(
            householdId = household.id,
            name = "iOS Smoke Milk $timestamp",
            category = dairyCategory.legacyCategory,
            categoryId = dairyCategory.id,
            quantity = 2.0,
            quantityUnit = QuantityUnit.PIECES,
            expirationDate = LocalDate.parse("2026-05-20"),
            brand = "Smoke Brand",
            ingredientsText = "milk",
            calories = 60.0,
            protein = 3.2,
            fat = 3.5,
            carbs = 4.8,
            remainingAmount = 0.4,
            lowStockThreshold = 1.0
        )

        val products = getProductsUseCase(household.id)
        val recipes = getRecipesUseCase(household.id)
        val settings = getNotificationSettingsUseCase()

        return IosRuntimeSmokeResult(
            email = email,
            userId = auth.userId,
            householdId = household.id,
            productId = product.id,
            productName = product.name,
            productsVisible = products.size,
            categoriesVisible = categories.size,
            suggestionSource = suggestion.source.name,
            recipeCount = recipes.size,
            pushEnabled = settings.pushEnabled
        )
    }

    suspend fun runRealtime(email: String, password: String, timestamp: String): IosRealtimeSmokeResult =
        coroutineScope {
            val auth = registerUseCase(email = email, password = password, name = "iOS Realtime Smoke")
            val household = createHouseholdUseCase("iOS Realtime $timestamp")
            val categories = getProductCategoriesUseCase(household.id)
            val dairyCategory = categories.firstOrNull { it.legacyCategory == ProductCategory.DAIRY }
                ?: categories.first()

            val realtimeEvent = async {
                withTimeout(25_000L) {
                    observeHouseholdEventsUseCase(household.id)
                        .filter { it.householdId == household.id }
                        .first()
                }
            }

            delay(3_000L)

            val product = addProductUseCase(
                householdId = household.id,
                name = "iOS Realtime Milk $timestamp",
                category = dairyCategory.legacyCategory,
                categoryId = dairyCategory.id,
                quantity = 1.0,
                quantityUnit = QuantityUnit.PIECES,
                expirationDate = LocalDate.parse("2026-05-21"),
                brand = "Realtime Brand",
                ingredientsText = "milk",
                calories = null,
                protein = null,
                fat = null,
                carbs = null,
                remainingAmount = 1.0,
                lowStockThreshold = 0.5
            )

            val event = realtimeEvent.await()
            val products = getProductsUseCase(household.id)

            IosRealtimeSmokeResult(
                email = email,
                userId = auth.userId,
                householdId = household.id,
                productId = product.id,
                productName = product.name,
                realtimeEventType = event.typeName(),
                realtimeEventReason = (event as? HouseholdRealtimeEvent.ResyncRequired)?.reason,
                productsVisible = products.size
            )
        }

    private fun HouseholdRealtimeEvent.typeName(): String =
        when (this) {
            is HouseholdRealtimeEvent.ProductCreated -> "PRODUCT_CREATED"
            is HouseholdRealtimeEvent.ProductUpdated -> "PRODUCT_UPDATED"
            is HouseholdRealtimeEvent.ProductDeleted -> "PRODUCT_DELETED"
            is HouseholdRealtimeEvent.ResyncRequired -> "RESYNC_REQUIRED"
        }
}
