package com.android.rut.miit.productinventory.feature.barcode.di

import com.android.rut.miit.productinventory.feature.barcode.api.AddBarcodeProductUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.BarcodeRepository
import com.android.rut.miit.productinventory.feature.barcode.api.LookupBarcodeUseCase
import com.android.rut.miit.productinventory.feature.barcode.api.ScanBarcodeUseCase
import com.android.rut.miit.productinventory.feature.barcode.data.BarcodeRemoteDataSource
import com.android.rut.miit.productinventory.feature.barcode.data.BarcodeRepositoryImpl
import com.android.rut.miit.productinventory.feature.barcode.presentation.BarcodeScanViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val barcodeModule = module {
    factory { BarcodeRemoteDataSource(get()) }
    factory<BarcodeRepository> { BarcodeRepositoryImpl(get(), get(), get()) }
    factoryOf(::LookupBarcodeUseCase)
    factoryOf(::AddBarcodeProductUseCase)
    factoryOf(::ScanBarcodeUseCase)
    viewModelOf(::BarcodeScanViewModel)
}
