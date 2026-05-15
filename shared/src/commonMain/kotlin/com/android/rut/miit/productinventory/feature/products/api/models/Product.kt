package com.android.rut.miit.productinventory.feature.products.api.models

import kotlinx.datetime.LocalDate

data class Product(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val category: ProductCategory,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val quantity: Double,
    val quantityUnit: QuantityUnit,
    val packageAmount: Double? = null,
    val packageUnit: QuantityUnit? = null,
    val ingredientsText: String? = null,
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null,
    val purchaseDate: LocalDate? = null,
    val remainingAmount: Double = quantity,
    val lowStockThreshold: Double? = null,
    val expirationDate: LocalDate?,
    val expirationStatus: ExpirationStatus,
    val householdId: String,
    val addedByUserId: String,
    val createdAt: String
)

enum class ProductCategory {
    DAIRY, MEAT_FISH, VEGETABLES_FRUITS, CEREALS, BEVERAGES, OTHER
}

data class ProductCategoryOption(
    val id: String,
    val householdId: String? = null,
    val code: ProductCategory? = null,
    val name: String,
    val system: Boolean,
    val archived: Boolean = false,
    val createdAt: String
) {
    val legacyCategory: ProductCategory = code ?: ProductCategory.OTHER

    companion object {
        const val DAIRY_SYSTEM_ID = "00000000-0000-0000-0000-000000000101"
        const val MEAT_FISH_SYSTEM_ID = "00000000-0000-0000-0000-000000000102"
        const val VEGETABLES_FRUITS_SYSTEM_ID = "00000000-0000-0000-0000-000000000103"
        const val CEREALS_SYSTEM_ID = "00000000-0000-0000-0000-000000000104"
        const val BEVERAGES_SYSTEM_ID = "00000000-0000-0000-0000-000000000105"
        const val OTHER_SYSTEM_ID = "00000000-0000-0000-0000-000000000106"

        fun systemDefaults(): List<ProductCategoryOption> = listOf(
            ProductCategoryOption(
                id = DAIRY_SYSTEM_ID,
                code = ProductCategory.DAIRY,
                name = "Dairy",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            ),
            ProductCategoryOption(
                id = MEAT_FISH_SYSTEM_ID,
                code = ProductCategory.MEAT_FISH,
                name = "Meat/Fish",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            ),
            ProductCategoryOption(
                id = VEGETABLES_FRUITS_SYSTEM_ID,
                code = ProductCategory.VEGETABLES_FRUITS,
                name = "Vegetables/Fruits",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            ),
            ProductCategoryOption(
                id = CEREALS_SYSTEM_ID,
                code = ProductCategory.CEREALS,
                name = "Cereals",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            ),
            ProductCategoryOption(
                id = BEVERAGES_SYSTEM_ID,
                code = ProductCategory.BEVERAGES,
                name = "Beverages",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            ),
            ProductCategoryOption(
                id = OTHER_SYSTEM_ID,
                code = ProductCategory.OTHER,
                name = "Other",
                system = true,
                createdAt = SYSTEM_CREATED_AT
            )
        )

        private const val SYSTEM_CREATED_AT = "2026-01-01T00:00:00Z"
    }
}

fun Product.customCategoryNameForDisplay(): String? =
    categoryName?.takeIf {
        category == ProductCategory.OTHER &&
            categoryId != null &&
            categoryId != ProductCategoryOption.OTHER_SYSTEM_ID
    }

enum class QuantityUnit {
    GRAMS, MILLILITERS, PIECES
}

enum class ExpirationStatus {
    FRESH, EXPIRING_SOON, EXPIRED, UNKNOWN
}
