package com.android.rut.miit.productinventory.feature.household.presentation.list

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.household.api.models.Household

sealed class HouseholdListState : UiState {
    data object Loading : HouseholdListState()
    data class Content(val households: List<Household>) : HouseholdListState()
    data class Error(val message: String?) : HouseholdListState()
}
