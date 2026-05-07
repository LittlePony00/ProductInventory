package com.android.rut.miit.productinventory.feature.products.data

import com.android.rut.miit.productinventory.feature.products.api.ProductRepository
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import com.android.rut.miit.productinventory.feature.products.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.products.data.models.CreateProductRequestDto
import com.android.rut.miit.productinventory.feature.products.data.models.UpdateProductRequestDto
import kotlinx.datetime.LocalDate

class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource
) : ProductRepository {

    override suspend fun getProducts(householdId: String): List<Product> {
        return remoteDataSource.getProducts(householdId).map { it.toDomain() }
    }

    override suspend fun getProduct(householdId: String, productId: String): Product {
        return remoteDataSource.getProduct(householdId, productId).toDomain()
    }

    override suspend fun addProduct(
        householdId: String,
        name: String,
        category: ProductCategory,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?
    ): Product {
        val request = CreateProductRequestDto(
            name = name,
            category = category.name,
            quantity = quantity,
            quantityUnit = quantityUnit.name,
            expirationDate = expirationDate?.toString()
        )
        return remoteDataSource.addProduct(householdId, request).toDomain()
    }

    override suspend fun updateProduct(
        householdId: String,
        productId: String,
        name: String?,
        category: ProductCategory?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?
    ): Product {
        val request = UpdateProductRequestDto(
            name = name,
            category = category?.name,
            quantity = quantity,
            quantityUnit = quantityUnit?.name,
            expirationDate = expirationDate?.toString()
        )
        return remoteDataSource.updateProduct(householdId, productId, request).toDomain()
    }

    override suspend fun deleteProduct(householdId: String, productId: String) {
        remoteDataSource.deleteProduct(householdId, productId)
    }

    override suspend fun getExpiringProducts(householdId: String, days: Int): List<Product> {
        return remoteDataSource.getExpiringProducts(householdId, days).map { it.toDomain() }
    }
}
