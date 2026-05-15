package com.android.rut.miit.productinventory.feature.products.api

import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProductCategoriesUseCaseTest {

    @Test
    fun `returns categories from repository`() = runTest {
        val categories = ProductCategoryOption.systemDefaults()

        assertEquals(categories, GetProductCategoriesUseCase(FakeCategoryRepository(categories))("household-id"))
    }

    private class FakeCategoryRepository(
        private val categories: List<ProductCategoryOption>
    ) : CategoryRepository {
        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            categories

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption =
            error("unused")

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption =
            error("unused")

        override suspend fun archiveCategory(householdId: String, categoryId: String) = Unit
    }
}
