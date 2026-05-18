package com.android.rut.miit.productinventory.domain.port.inbound

import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import java.io.InputStream
import java.time.LocalDate
import java.util.UUID

interface IProductService {
    fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        brand: String?,
        barcode: String?,
        category: ProductCategory,
        categoryId: UUID?,
        quantity: Double,
        quantityUnit: QuantityUnit,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product =
        addProduct(
            userId = userId,
            householdId = householdId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            imageUrl = null,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate
        )

    fun addProduct(
        userId: UUID,
        householdId: UUID,
        name: String,
        brand: String?,
        barcode: String?,
        category: ProductCategory,
        categoryId: UUID?,
        quantity: Double,
        quantityUnit: QuantityUnit,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        imageUrl: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product =
        addProduct(
            userId = userId,
            householdId = householdId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate
        )

    fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        brand: String?,
        barcode: String?,
        category: ProductCategory?,
        categoryId: UUID?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product =
        updateProduct(
            userId = userId,
            productId = productId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            imageUrl = null,
            clearImage = false,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate
        )

    fun updateProduct(
        userId: UUID,
        productId: UUID,
        name: String?,
        brand: String?,
        barcode: String?,
        category: ProductCategory?,
        categoryId: UUID?,
        quantity: Double?,
        quantityUnit: QuantityUnit?,
        packageAmount: Double?,
        packageUnit: QuantityUnit?,
        ingredientsText: String?,
        imageUrl: String?,
        clearImage: Boolean,
        calories: Double?,
        protein: Double?,
        fat: Double?,
        carbs: Double?,
        purchaseDate: LocalDate?,
        remainingAmount: Double?,
        lowStockThreshold: Double?,
        expirationDate: LocalDate?
    ): Product =
        updateProduct(
            userId = userId,
            productId = productId,
            name = name,
            brand = brand,
            barcode = barcode,
            category = category,
            categoryId = categoryId,
            quantity = quantity,
            quantityUnit = quantityUnit,
            packageAmount = packageAmount,
            packageUnit = packageUnit,
            ingredientsText = ingredientsText,
            calories = calories,
            protein = protein,
            fat = fat,
            carbs = carbs,
            purchaseDate = purchaseDate,
            remainingAmount = remainingAmount,
            lowStockThreshold = lowStockThreshold,
            expirationDate = expirationDate
        )

    fun consumeProduct(userId: UUID, productId: UUID, amount: Double): Product
    fun uploadProductImage(
        userId: UUID,
        productId: UUID,
        originalFilename: String?,
        contentType: String?,
        size: Long,
        inputStream: InputStream
    ): Product = throw UnsupportedOperationException("Product image upload is not implemented")

    fun deleteProductImage(userId: UUID, productId: UUID): Product =
        throw UnsupportedOperationException("Product image deletion is not implemented")

    fun deleteProduct(userId: UUID, productId: UUID)
    fun getProducts(userId: UUID, householdId: UUID, categoryId: UUID? = null): List<Product>
    fun getProduct(userId: UUID, productId: UUID): Product
    fun getExpiringProducts(userId: UUID, householdId: UUID, days: Int = 3): List<Product>
}
