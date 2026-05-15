package com.android.rut.miit.productinventory.feature.products.presentation.categories

import com.android.rut.miit.productinventory.feature.products.api.ArchiveProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.CategoryRepository
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryManagementViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `creates updates and archives custom category`() = runTest {
        val repository = FakeCategoryRepository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(CategoryManagementEvent.OnCreate("household-id"))
        advanceUntilIdle()
        viewModel.onEvent(CategoryManagementEvent.OnNewCategoryNameChanged("Bakery"))
        viewModel.onEvent(CategoryManagementEvent.OnCreateCategoryClick)
        advanceUntilIdle()
        val categoryId = viewModel.viewState.value.categories.single { !it.system }.id

        viewModel.onEvent(CategoryManagementEvent.OnCategoryNameChanged(categoryId, "Bread"))
        viewModel.onEvent(CategoryManagementEvent.OnUpdateCategoryClick(categoryId))
        advanceUntilIdle()
        viewModel.onEvent(CategoryManagementEvent.OnArchiveCategoryClick(categoryId))
        advanceUntilIdle()

        assertEquals(ProductCategoryOption.systemDefaults().size, viewModel.viewState.value.categories.size)
        assertEquals(listOf("Bakery"), repository.createdNames)
        assertEquals(listOf("Bread"), repository.updatedNames)
        assertEquals(listOf(categoryId), repository.archivedIds)
    }

    private fun viewModel(repository: CategoryRepository): CategoryManagementViewModel =
        CategoryManagementViewModel(
            getProductCategoriesUseCase = GetProductCategoriesUseCase(repository),
            createProductCategoryUseCase = CreateProductCategoryUseCase(repository),
            updateProductCategoryUseCase = UpdateProductCategoryUseCase(repository),
            archiveProductCategoryUseCase = ArchiveProductCategoryUseCase(repository)
        )

    private class FakeCategoryRepository : CategoryRepository {
        private var categories = ProductCategoryOption.systemDefaults()
        val createdNames = mutableListOf<String>()
        val updatedNames = mutableListOf<String>()
        val archivedIds = mutableListOf<String>()

        override suspend fun getCategories(householdId: String, includeArchived: Boolean): List<ProductCategoryOption> =
            categories.filter { includeArchived || !it.archived }

        override suspend fun createCategory(householdId: String, name: String): ProductCategoryOption {
            createdNames += name
            val category = ProductCategoryOption(
                id = "custom-${createdNames.size}",
                householdId = householdId,
                name = name,
                system = false,
                createdAt = "2026-05-14T00:00:00Z"
            )
            categories += category
            return category
        }

        override suspend fun updateCategory(householdId: String, categoryId: String, name: String): ProductCategoryOption {
            updatedNames += name
            val updated = categories.first { it.id == categoryId }.copy(name = name)
            categories = categories.map { if (it.id == categoryId) updated else it }
            return updated
        }

        override suspend fun archiveCategory(householdId: String, categoryId: String) {
            archivedIds += categoryId
            categories = categories.map { if (it.id == categoryId) it.copy(archived = true) else it }
        }
    }
}
