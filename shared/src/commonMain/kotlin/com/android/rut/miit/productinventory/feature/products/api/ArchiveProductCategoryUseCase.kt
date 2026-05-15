package com.android.rut.miit.productinventory.feature.products.api

class ArchiveProductCategoryUseCase(private val repository: CategoryRepository) {
    suspend operator fun invoke(householdId: String, categoryId: String) {
        repository.archiveCategory(householdId, categoryId)
    }
}
