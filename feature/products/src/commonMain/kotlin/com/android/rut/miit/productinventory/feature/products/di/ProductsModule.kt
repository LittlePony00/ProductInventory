package com.android.rut.miit.productinventory.feature.products.di

import com.android.rut.miit.productinventory.feature.products.api.AddProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.ArchiveProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.ApplyRealtimeProductEventUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.ConsumeProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.DeleteProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductsUseCase
import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.SuggestProductEnrichmentUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductUseCase
import com.android.rut.miit.productinventory.feature.products.data.CategoryRemoteDataSource
import com.android.rut.miit.productinventory.feature.products.data.CategoryRepositoryImpl
import com.android.rut.miit.productinventory.feature.products.data.ProductRemoteDataSource
import com.android.rut.miit.productinventory.feature.products.data.ProductRepositoryImpl
import com.android.rut.miit.productinventory.feature.products.presentation.categories.CategoryManagementViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.add.AddProductViewModel
import com.android.rut.miit.productinventory.feature.products.presentation.list.ProductListViewModel
import com.android.rut.miit.productinventory.feature.realtime.di.realtimeModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

expect fun productPlatformModule(): Module

val productsModule = module {
    includes(realtimeModule, productPlatformModule())
    factory { ProductRemoteDataSource(get()) }
    factory { CategoryRemoteDataSource(get()) }
    single<ProductRepository> { ProductRepositoryImpl(get(), get(), get(), get()) }
    factory<CategoryRepository> { CategoryRepositoryImpl(get()) }
    factoryOf(::GetProductsUseCase)
    factoryOf(::GetProductUseCase)
    factoryOf(::GetProductCategoriesUseCase)
    factoryOf(::CreateProductCategoryUseCase)
    factoryOf(::UpdateProductCategoryUseCase)
    factoryOf(::ArchiveProductCategoryUseCase)
    factoryOf(::AddProductUseCase)
    factoryOf(::UpdateProductUseCase)
    factoryOf(::ConsumeProductUseCase)
    factoryOf(::SuggestProductEnrichmentUseCase)
    factoryOf(::ApplyRealtimeProductEventUseCase)
    factoryOf(::DeleteProductUseCase)
    viewModelOf(::ProductListViewModel)
    viewModelOf(::AddProductViewModel)
    viewModelOf(::CategoryManagementViewModel)
}
