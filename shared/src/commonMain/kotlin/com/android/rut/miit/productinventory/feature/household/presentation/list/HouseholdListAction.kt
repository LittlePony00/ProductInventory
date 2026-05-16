package com.android.rut.miit.productinventory.feature.household.presentation.list

import com.android.rut.miit.productinventory.common.UiAction

sealed class HouseholdListAction : UiAction {
    data class OpenHousehold(val householdId: String) : HouseholdListAction()
    data object ShowCreateDialog : HouseholdListAction()
    data object ShowJoinDialog : HouseholdListAction()
    data object CloseJoinDialog : HouseholdListAction()
    data class ShowInviteCode(val code: String, val expiresAt: String) : HouseholdListAction()
    data class ShowMessage(val message: String) : HouseholdListAction()
    data object OpenProfile : HouseholdListAction()
}
