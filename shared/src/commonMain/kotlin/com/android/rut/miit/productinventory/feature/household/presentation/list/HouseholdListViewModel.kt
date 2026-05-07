package com.android.rut.miit.productinventory.feature.household.presentation.list

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.household.api.CreateHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.JoinHouseholdUseCase
import kotlinx.coroutines.launch

class HouseholdListViewModel(
    private val getHouseholdsUseCase: GetHouseholdsUseCase,
    private val createHouseholdUseCase: CreateHouseholdUseCase,
    private val joinHouseholdUseCase: JoinHouseholdUseCase
) : SharedViewModel<HouseholdListState, HouseholdListEvent, HouseholdListAction>(
    initialState = HouseholdListState.Loading
) {

    override suspend fun handleEvent(event: HouseholdListEvent) {
        when (event) {
            is HouseholdListEvent.OnCreate -> loadHouseholds()
            is HouseholdListEvent.OnRetry -> loadHouseholds()
            is HouseholdListEvent.OnHouseholdClick ->
                sendAction(HouseholdListAction.OpenHousehold(event.householdId))
            is HouseholdListEvent.OnCreateHouseholdClick ->
                sendAction(HouseholdListAction.ShowCreateDialog)
            is HouseholdListEvent.OnJoinHouseholdClick ->
                sendAction(HouseholdListAction.ShowJoinDialog)
            is HouseholdListEvent.OnCreateHouseholdConfirm -> createHousehold(event.name)
            is HouseholdListEvent.OnJoinHouseholdConfirm -> joinHousehold(event.inviteCode)
            is HouseholdListEvent.OnProfileClick ->
                sendAction(HouseholdListAction.OpenProfile)
        }
    }

    private fun loadHouseholds() {
        viewModelScope.launch {
            updateState { HouseholdListState.Loading }
            runCatching { getHouseholdsUseCase() }
                .onSuccess { households ->
                    updateState { HouseholdListState.Content(households) }
                }
                .onFailure { error ->
                    updateState { HouseholdListState.Error(error.message) }
                }
        }
    }

    private fun createHousehold(name: String) {
        viewModelScope.launch {
            runCatching { createHouseholdUseCase(name) }
                .onSuccess {
                    sendAction(HouseholdListAction.ShowMessage("Домохозяйство создано"))
                    loadHouseholds()
                }
                .onFailure { error ->
                    sendAction(HouseholdListAction.ShowMessage(error.message ?: "Ошибка"))
                }
        }
    }

    private fun joinHousehold(inviteCode: String) {
        viewModelScope.launch {
            runCatching { joinHouseholdUseCase(inviteCode) }
                .onSuccess {
                    sendAction(HouseholdListAction.ShowMessage("Вы присоединились"))
                    loadHouseholds()
                }
                .onFailure { error ->
                    sendAction(HouseholdListAction.ShowMessage(error.message ?: "Неверный код"))
                }
        }
    }
}
