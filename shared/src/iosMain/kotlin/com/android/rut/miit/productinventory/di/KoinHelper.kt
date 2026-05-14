package com.android.rut.miit.productinventory.di

import com.android.rut.miit.productinventory.core.di.appModules
import com.android.rut.miit.productinventory.feature.auth.presentation.login.LoginViewModel
import com.android.rut.miit.productinventory.feature.auth.presentation.register.RegisterViewModel
import com.android.rut.miit.productinventory.feature.household.presentation.list.HouseholdListViewModel
import com.android.rut.miit.productinventory.feature.notifications.presentation.NotificationListViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.add.AddProductViewModel
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
    fun profileViewModel(): ProfileViewModel = get()
    fun notificationListViewModel(): NotificationListViewModel = get()
    fun recipeListViewModel(): RecipeListViewModel = get()
    fun barcodeScanViewModel(): BarcodeScanViewModel = get()
}
