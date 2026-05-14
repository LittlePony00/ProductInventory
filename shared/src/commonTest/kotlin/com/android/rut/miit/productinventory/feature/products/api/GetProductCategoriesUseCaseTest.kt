package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProductCategoriesUseCaseTest {

    @Test
    fun `returns all supported product categories`() {
        assertEquals(ProductCategory.entries, GetProductCategoriesUseCase()())
    }
}
