package com.android.rut.miit.productinventory.data.local.adapter

import com.android.rut.miit.productinventory.core.local.ProductLocalDataSource
import com.android.rut.miit.productinventory.data.local.dao.ProductDao
import com.android.rut.miit.productinventory.data.local.entity.ProductLocalEntity
import com.android.rut.miit.productinventory.feature.products.api.models.ExpirationStatus
import com.android.rut.miit.productinventory.feature.products.api.models.Product
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import com.android.rut.miit.productinventory.feature.products.api.models.QuantityUnit
import kotlinx.datetime.LocalDate

class RoomProductLocalDataSource(
    private val productDao: ProductDao
) : ProductLocalDataSource {

    override suspend fun getProducts(householdId: String): List<Product> {
        return productDao.getByHouseholdId(householdId).map { it.toDomain() }
    }

    override suspend fun saveProducts(householdId: String, products: List<Product>) {
        productDao.deleteAllByHouseholdId(householdId)
        productDao.insertAll(products.map { it.toEntity() })
    }

    override suspend fun getProductByBarcode(barcode: String): Product? {
        return productDao.getByBarcode(barcode)?.toDomain()
    }

    override suspend fun deleteProduct(id: String) {
        productDao.deleteById(id)
    }

    override suspend fun saveProduct(product: Product) {
        productDao.insert(product.toEntity())
    }

    private fun ProductLocalEntity.toDomain() = Product(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        category = runCatching { ProductCategory.valueOf(category) }.getOrDefault(ProductCategory.OTHER),
        quantity = quantity,
        quantityUnit = runCatching { QuantityUnit.valueOf(quantityUnit) }.getOrDefault(QuantityUnit.PIECES),
        packageAmount = packageAmount,
        packageUnit = packageUnit?.let { runCatching { QuantityUnit.valueOf(it) }.getOrNull() },
        ingredientsText = ingredientsText,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        purchaseDate = purchaseDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        expirationDate = expirationDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        expirationStatus = runCatching { ExpirationStatus.valueOf(expirationStatus) }.getOrDefault(ExpirationStatus.UNKNOWN),
        householdId = householdId,
        addedByUserId = addedByUserId,
        createdAt = createdAt
    )

    private fun Product.toEntity() = ProductLocalEntity(
        id = id,
        name = name,
        brand = brand,
        barcode = barcode,
        category = category.name,
        quantity = quantity,
        quantityUnit = quantityUnit.name,
        packageAmount = packageAmount,
        packageUnit = packageUnit?.name,
        ingredientsText = ingredientsText,
        calories = calories,
        protein = protein,
        fat = fat,
        carbs = carbs,
        purchaseDate = purchaseDate?.toString(),
        remainingAmount = remainingAmount,
        lowStockThreshold = lowStockThreshold,
        expirationDate = expirationDate?.toString(),
        expirationStatus = expirationStatus.name,
        householdId = householdId,
        addedByUserId = addedByUserId,
        createdAt = createdAt
    )
}
