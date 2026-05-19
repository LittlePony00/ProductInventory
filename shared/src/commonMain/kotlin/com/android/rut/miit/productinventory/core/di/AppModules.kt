package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.feature.auth.di.authModule
import com.android.rut.miit.productinventory.feature.barcode.di.barcodeModule
import com.android.rut.miit.productinventory.feature.household.di.householdModule
import com.android.rut.miit.productinventory.feature.notifications.di.notificationsModule
import com.android.rut.miit.productinventory.feature.products.di.productsModule
import com.android.rut.miit.productinventory.feature.profile.di.profileModule
import com.android.rut.miit.productinventory.feature.recommendations.di.recommendationsModule

val appModules = listOf(
    platformModule(),
    localDataSourceModule(),
    coreModule,
    authModule,
    productsModule,
    householdModule,
    barcodeModule,
    profileModule,
    notificationsModule,
    recommendationsModule
)
