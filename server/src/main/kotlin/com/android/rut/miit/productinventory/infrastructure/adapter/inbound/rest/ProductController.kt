package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.CreateProductRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateProductRequest
import com.android.rut.miit.productinventory.application.dto.response.ProductResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IProductService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households/{householdId}/products")
class ProductController(
    private val productService: IProductService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun addProduct(
        @PathVariable householdId: UUID,
        @Valid @RequestBody request: CreateProductRequest
    ): ProductResponse {
        return productService.addProduct(
            userId = currentUserId(),
            householdId = householdId,
            name = request.name,
            brand = request.brand,
            barcode = request.barcode,
            category = request.category,
            categoryId = request.categoryId,
            quantity = request.quantity,
            quantityUnit = request.quantityUnit,
            packageAmount = request.packageAmount,
            packageUnit = request.packageUnit,
            ingredientsText = request.ingredientsText,
            calories = request.calories,
            protein = request.protein,
            fat = request.fat,
            carbs = request.carbs,
            purchaseDate = request.purchaseDate,
            remainingAmount = request.remainingAmount,
            lowStockThreshold = request.lowStockThreshold,
            expirationDate = request.expirationDate
        ).toResponse()
    }

    @GetMapping
    fun getProducts(
        @PathVariable householdId: UUID,
        @RequestParam(required = false) categoryId: UUID?
    ): List<ProductResponse> {
        return productService.getProducts(currentUserId(), householdId, categoryId).map { it.toResponse() }
    }

    @GetMapping("/{productId}")
    fun getProduct(
        @PathVariable householdId: UUID,
        @PathVariable productId: UUID
    ): ProductResponse {
        return productService.getProduct(currentUserId(), productId).toResponse()
    }

    @PutMapping("/{productId}")
    fun updateProduct(
        @PathVariable householdId: UUID,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateProductRequest
    ): ProductResponse {
        return productService.updateProduct(
            userId = currentUserId(),
            productId = productId,
            name = request.name,
            brand = request.brand,
            barcode = request.barcode,
            category = request.category,
            categoryId = request.categoryId,
            quantity = request.quantity,
            quantityUnit = request.quantityUnit,
            packageAmount = request.packageAmount,
            packageUnit = request.packageUnit,
            ingredientsText = request.ingredientsText,
            calories = request.calories,
            protein = request.protein,
            fat = request.fat,
            carbs = request.carbs,
            purchaseDate = request.purchaseDate,
            remainingAmount = request.remainingAmount,
            lowStockThreshold = request.lowStockThreshold,
            expirationDate = request.expirationDate
        ).toResponse()
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(
        @PathVariable householdId: UUID,
        @PathVariable productId: UUID
    ) {
        productService.deleteProduct(currentUserId(), productId)
    }

    @GetMapping("/expiring")
    fun getExpiringProducts(
        @PathVariable householdId: UUID,
        @RequestParam(defaultValue = "3") days: Int
    ): List<ProductResponse> {
        return productService.getExpiringProducts(currentUserId(), householdId, days)
            .map { it.toResponse() }
    }
}
