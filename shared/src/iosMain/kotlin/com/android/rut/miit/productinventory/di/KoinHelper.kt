package com.android.rut.miit.productinventory.di

import com.android.rut.miit.productinventory.core.di.appModules
import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.push.IosPushTokenBridge
import com.android.rut.miit.productinventory.feature.auth.api.RestoreSessionUseCase
import com.android.rut.miit.productinventory.feature.auth.api.ValidateSessionUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.auth.presentation.login.LoginViewModel
import com.android.rut.miit.productinventory.feature.auth.presentation.register.RegisterViewModel
import com.android.rut.miit.productinventory.feature.household.presentation.list.HouseholdListViewModel
import com.android.rut.miit.productinventory.feature.notifications.presentation.NotificationListViewModel
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings
import com.android.rut.miit.productinventory.feature.products.api.OutboxSyncCoordinator
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminder
import com.android.rut.miit.productinventory.feature.products.api.ProductLocalReminderPlanner
import com.android.rut.miit.productinventory.feature.products.presentation.add.AddProductViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.categories.CategoryManagementViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.list.ProductListViewModel
import com.android.rut.miit.productinventory.feature.profile.presentation.ProfileViewModel
import com.android.rut.miit.productinventory.feature.barcode.presentation.BarcodeScanViewModel
import com.android.rut.miit.productinventory.feature.recommendations.presentation.RecipeListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(appModules)
    }
}

class KoinHelper : KoinComponent {
    fun loginViewModel(): LoginViewModel = get()
    fun registerViewModel(): RegisterViewModel = get()
    fun householdListViewModel(): HouseholdListViewModel = get()
    fun productListViewModel(): ProductListViewModel = get()
    fun addProductViewModel(): AddProductViewModel = get()
    fun categoryManagementViewModel(): CategoryManagementViewModel = get()
    fun profileViewModel(): ProfileViewModel = get()
    fun notificationListViewModel(): NotificationListViewModel = get()
    fun recipeListViewModel(): RecipeListViewModel = get()
    fun barcodeScanViewModel(): BarcodeScanViewModel = get()
    suspend fun restoreSession(): Boolean = get<RestoreSessionUseCase>().invoke()
    suspend fun validateSession(): Boolean = get<ValidateSessionUseCase>().invoke()
    suspend fun registerCurrentDeviceToken() {
        get<DeviceTokenRegistrar>().registerCurrentToken()
    }

    fun cacheIosPushToken(token: String) {
        IosPushTokenBridge.setCurrentToken(token)
    }

    suspend fun registerIosPushToken(token: String) {
        get<DeviceTokenRegistrar>().registerToken(token)
    }

    suspend fun syncCachedOutbox() {
        val coordinator = get<OutboxSyncCoordinator>()
        get<GetHouseholdsUseCase>().invoke().forEach { household ->
            runCatching { coordinator.sync(household.id) }
        }
    }

    suspend fun currentProductLocalReminders(): List<ProductLocalReminder> {
        val productLocalDataSource = get<ProductLocalDataSource>()
        val settings = runCatching { get<GetNotificationSettingsUseCase>().invoke() }
            .getOrDefault(NotificationSettings())
        val products = get<HouseholdLocalDataSource>().getHouseholds()
            .flatMap { household -> productLocalDataSource.getProducts(household.id) }
        return ProductLocalReminderPlanner().plan(products, settings)
    }
}
