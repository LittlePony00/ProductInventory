package com.android.rut.miit.productinventory.feature.products.presentation.categories

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.products.api.ArchiveProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.CreateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.GetProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.RefreshProductCategoriesUseCase
import com.android.rut.miit.productinventory.feature.products.api.UpdateProductCategoryUseCase
import com.android.rut.miit.productinventory.feature.products.api.models.ProductCategoryOption
import kotlinx.coroutines.launch

class CategoryManagementViewModel(
    private val getProductCategoriesUseCase: GetProductCategoriesUseCase,
    private val refreshProductCategoriesUseCase: RefreshProductCategoriesUseCase,
    private val createProductCategoryUseCase: CreateProductCategoryUseCase,
    private val updateProductCategoryUseCase: UpdateProductCategoryUseCase,
    private val archiveProductCategoryUseCase: ArchiveProductCategoryUseCase
) : SharedViewModel<CategoryManagementState, CategoryManagementEvent, CategoryManagementAction>(
    initialState = CategoryManagementState()
) {
    private var householdId: String = ""

    override suspend fun handleEvent(event: CategoryManagementEvent) {
        when (event) {
            is CategoryManagementEvent.OnCreate -> onCreate(event.householdId)
            is CategoryManagementEvent.OnRetry -> loadCategories()
            is CategoryManagementEvent.OnNewCategoryNameChanged ->
                updateState { copy(newCategoryName = event.name) }
            is CategoryManagementEvent.OnCreateCategoryClick -> createCategory()
            is CategoryManagementEvent.OnCategoryNameChanged ->
                updateState { copy(editableNames = editableNames + (event.categoryId to event.name)) }
            is CategoryManagementEvent.OnUpdateCategoryClick -> updateCategory(event.categoryId)
            is CategoryManagementEvent.OnArchiveCategoryClick -> archiveCategory(event.categoryId)
        }
    }

    private fun onCreate(householdId: String) {
        if (this.householdId == householdId && currentState.categories.isNotEmpty()) return
        this.householdId = householdId
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            runCatching { getProductCategoriesUseCase(householdId) }
                .onSuccess { categories ->
                    updateState {
                        copy(
                            categories = categories.sortForDisplay(),
                            editableNames = categories.associate { it.id to it.name },
                            isLoading = false
                        )
                    }
                    refreshCategoriesSilently()
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error.message) }
                    sendAction(CategoryManagementAction.ShowError(error.message ?: "Ошибка"))
                }
        }
    }

    private fun refreshCategoriesSilently() {
        viewModelScope.launch {
            runCatching { refreshProductCategoriesUseCase(householdId) }
                .onSuccess { categories ->
                    updateState {
                        copy(
                            categories = categories.sortForDisplay(),
                            editableNames = categories.associate { it.id to it.name },
                            isLoading = false,
                            error = null
                        )
                    }
                }
        }
    }

    private fun createCategory() {
        val name = currentState.newCategoryName.trim()
        if (name.isBlank()) {
            showValidationError("Введите название категории")
            return
        }
        viewModelScope.launch {
            updateState { copy(isSaving = true, error = null) }
            runCatching { createProductCategoryUseCase(householdId, name) }
                .onSuccess { category ->
                    updateState {
                        val nextCategories = (categories.filterNot { it.id == category.id } + category).sortForDisplay()
                        copy(
                            categories = nextCategories,
                            editableNames = nextCategories.associate { it.id to it.name },
                            newCategoryName = "",
                            isSaving = false
                        )
                    }
                }
                .onFailure { error -> showMutationError(error) }
        }
    }

    private fun updateCategory(categoryId: String) {
        val category = currentState.categories.firstOrNull { it.id == categoryId } ?: return
        if (category.system) return
        val name = currentState.editableNames[categoryId]?.trim().orEmpty()
        if (name.isBlank()) {
            showValidationError("Введите название категории")
            return
        }
        viewModelScope.launch {
            updateState { copy(isSaving = true, error = null) }
            runCatching { updateProductCategoryUseCase(householdId, categoryId, name) }
                .onSuccess { updated ->
                    updateState {
                        val nextCategories = categories.map { if (it.id == updated.id) updated else it }.sortForDisplay()
                        copy(
                            categories = nextCategories,
                            editableNames = nextCategories.associate { it.id to it.name },
                            isSaving = false
                        )
                    }
                }
                .onFailure { error -> showMutationError(error) }
        }
    }

    private fun archiveCategory(categoryId: String) {
        val category = currentState.categories.firstOrNull { it.id == categoryId } ?: return
        if (category.system) return
        viewModelScope.launch {
            updateState { copy(isSaving = true, error = null) }
            runCatching { archiveProductCategoryUseCase(householdId, categoryId) }
                .onSuccess {
                    updateState {
                        val nextCategories = categories.filterNot { it.id == categoryId }
                        copy(
                            categories = nextCategories,
                            editableNames = editableNames - categoryId,
                            isSaving = false
                        )
                    }
                }
                .onFailure { error -> showMutationError(error) }
        }
    }

    private fun showValidationError(message: String) {
        updateState { copy(error = message, isSaving = false) }
        sendAction(CategoryManagementAction.ShowError(message))
    }

    private fun showMutationError(error: Throwable) {
        val message = error.message ?: "Ошибка"
        updateState { copy(isSaving = false, error = message) }
        sendAction(CategoryManagementAction.ShowError(message))
    }

    private fun List<ProductCategoryOption>.sortForDisplay(): List<ProductCategoryOption> =
        sortedWith(compareBy<ProductCategoryOption> { !it.system }.thenBy { it.name.lowercase() })
}
