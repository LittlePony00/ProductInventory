package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import java.time.LocalDate
import java.util.UUID

interface IProductService {
    fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        category: ProductCategory,
        quantity: Double,
        quantityUnit: QuantityUnit,
        expirationDate: LocalDate?
    ): Product

    fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        category: ProductCategory?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        expirationDate: LocalDate?
    ): Product

    fun deleteProduct(userId: UUID, productId: UUID)
    fun getProducts(userId: UUID, householdId: UUID): List<Product>
    fun getProduct(userId: UUID, productId: UUID): Product
    fun getExpiringProducts(userId: UUID, householdId: UUID, days: Int = 3): List<Product>
}
