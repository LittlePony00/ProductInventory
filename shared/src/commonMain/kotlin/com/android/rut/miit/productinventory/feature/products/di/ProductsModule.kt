package com.android.rut.miit.productinventory.feature.products.di

import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.data.ProductRemoteDataSource
import com.android.rut.miit.productinventory.feature.products.data.ProductRepositoryImpl
import com.android.rut.miit.productinventory.feature.products.presentation.add.AddProductViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.list.ProductListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val productsModule = module {
    factory { ProductRemoteDataSource(get()) }
    factory<ProductRepository> { ProductRepositoryImpl(get()) }
    factoryOf(::GetProductsUseCase)
    factoryOf(::AddProductUseCase)
    factoryOf(::DeleteProductUseCase)
    viewModelOf(::ProductListViewModel)
    viewModelOf(::AddProductViewModel)
}
