package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.application.dto.response.ProductResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.exception.BarcodeNotFoundException
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeProductService
import com.android.rut.miit.productinventory.domain.port.inbound.IBarcodeService
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BarcodeServiceImpl(
    private val barcodeProductService: IBarcodeProductService,
    private val productService: IProductService
) : IBarcodeService {

    @Transactional
    override fun lookupAndAddProduct(householdId: String, userId: String, barcode: String): ProductResponse {
        val parsedUserId = UUID.fromString(userId)
        val parsedHouseholdId = UUID.fromString(householdId)
        val normalizedBarcode = barcode.trim()
        val draft = barcodeProductService.getProductDraft(parsedUserId, parsedHouseholdId, normalizedBarcode)
        val name = draft.name?.trimToNull() ?: throw BarcodeNotFoundException(normalizedBarcode)

        return productService.addProduct(
            userId = parsedUserId,
            householdId = parsedHouseholdId,
            name = name,
            brand = draft.brand,
            barcode = normalizedBarcode,
            category = draft.category ?: ProductCategory.OTHER,
            categoryId = null,
            quantity = 1.0,
            quantityUnit = QuantityUnit.PIECES,
            packageAmount = draft.packageQuantity?.value,
            packageUnit = draft.packageQuantity?.unit,
            ingredientsText = draft.ingredients,
            calories = draft.nutrition?.caloriesKcal,
            protein = draft.nutrition?.proteinGrams,
            fat = draft.nutrition?.fatGrams,
            carbs = draft.nutrition?.carbohydratesGrams,
            purchaseDate = null,
            remainingAmount = null,
            lowStockThreshold = null,
            expirationDate = null
        ).toResponse()
    }
}

private fun String.trimToNull(): String? = trim().takeIf { it.isNotEmpty() }
