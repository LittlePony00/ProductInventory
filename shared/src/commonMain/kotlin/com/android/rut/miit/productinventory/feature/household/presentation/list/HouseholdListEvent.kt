package com.android.rut.miit.productinventory.feature.household.presentation.list

import com.android.rut.miit.productinventory.common.UiEvent

sealed class HouseholdListEvent : UiEvent {
    data object OnCreate : HouseholdListEvent()
    data object OnRetry : HouseholdListEvent()
    data class OnHouseholdClick(val householdId: String) : HouseholdListEvent()
    data object OnCreateHouseholdClick : HouseholdListEvent()
    data object OnJoinHouseholdClick : HouseholdListEvent()
    data class OnGenerateInviteCodeClick(val householdId: String) : HouseholdListEvent()
    data class OnCreateHouseholdConfirm(val name: String) : HouseholdListEvent()
    data class OnJoinHouseholdConfirm(val inviteCode: String) : HouseholdListEvent()
    data object OnProfileClick : HouseholdListEvent()
}
