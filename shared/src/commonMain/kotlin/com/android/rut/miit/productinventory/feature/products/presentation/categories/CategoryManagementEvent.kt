package com.android.rut.miit.productinventory.feature.products.presentation.categories

import com.android.rut.miit.productinventory.common.UiEvent

sealed class CategoryManagementEvent : UiEvent {
    data class OnCreate(val householdId: String) : CategoryManagementEvent()
    data object OnRetry : CategoryManagementEvent()
    data class OnNewCategoryNameChanged(val name: String) : CategoryManagementEvent()
    data object OnCreateCategoryClick : CategoryManagementEvent()
    data class OnCategoryNameChanged(val categoryId: String, val name: String) : CategoryManagementEvent()
    data class OnUpdateCategoryClick(val categoryId: String) : CategoryManagementEvent()
    data class OnArchiveCategoryClick(val categoryId: String) : CategoryManagementEvent()
}
