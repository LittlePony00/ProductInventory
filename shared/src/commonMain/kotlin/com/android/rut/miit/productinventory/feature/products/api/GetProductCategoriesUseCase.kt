package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory

class GetProductCategoriesUseCase {
    operator fun invoke(): List<ProductCategory> = ProductCategory.entries
}
