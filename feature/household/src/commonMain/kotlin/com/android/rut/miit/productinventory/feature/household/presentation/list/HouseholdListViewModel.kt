package com.android.rut.miit.productinventory.feature.household.presentation.list

import androidx.lifecycle.viewModelScope
import com.android.rut.miit.productinventory.common.SharedViewModel
import com.android.rut.miit.productinventory.feature.household.api.CreateHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.GenerateInviteCodeUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.JoinHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.RefreshHouseholdsUseCase
import kotlinx.coroutines.launch

class HouseholdListViewModel(
    private val getHouseholdsUseCase: GetHouseholdsUseCase,
    private val refreshHouseholdsUseCase: RefreshHouseholdsUseCase,
    private val createHouseholdUseCase: CreateHouseholdUseCase,
    private val generateInviteCodeUseCase: GenerateInviteCodeUseCase,
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
            is HouseholdListEvent.OnGenerateInviteCodeClick ->
                generateInviteCode(event.householdId)
            is HouseholdListEvent.OnCreateHouseholdConfirm -> createHousehold(event.name)
            is HouseholdListEvent.OnJoinHouseholdConfirm -> joinHousehold(event.inviteCode)
            is HouseholdListEvent.OnProfileClick ->
                sendAction(HouseholdListAction.OpenProfile)
        }
    }

    private fun loadHouseholds() {
        viewModelScope.launch {
            val hadContent = currentState is HouseholdListState.Content
            if (!hadContent) {
                updateState { HouseholdListState.Loading }
            }
            runCatching { getHouseholdsUseCase() }
                .onSuccess { households ->
                    updateState { HouseholdListState.Content(households) }
                }
                .onFailure { error ->
                    if (!hadContent) {
                        updateState { HouseholdListState.Error(error.message) }
                    }
                }
            refreshHouseholds()
        }
    }

    private fun refreshHouseholds() {
        viewModelScope.launch {
            runCatching { refreshHouseholdsUseCase() }
                .onSuccess { households ->
                    updateState { HouseholdListState.Content(households) }
                }
                .onFailure { error ->
                    if (currentState !is HouseholdListState.Content) {
                        updateState { HouseholdListState.Error(error.message) }
                    }
                }
        }
    }

    private fun generateInviteCode(householdId: String) {
        viewModelScope.launch {
            runCatching { generateInviteCodeUseCase(householdId) }
                .onSuccess { inviteCode ->
                    sendAction(
                        HouseholdListAction.ShowInviteCode(
                            code = inviteCode.code,
                            expiresAt = inviteCode.expiresAt
                        )
                    )
                }
                .onFailure { error ->
                    sendAction(HouseholdListAction.ShowMessage(error.message ?: "Ошибка приглашения"))
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
                    sendAction(HouseholdListAction.CloseJoinDialog)
                    sendAction(HouseholdListAction.ShowMessage("Вы присоединились"))
                    loadHouseholds()
                }
                .onFailure { error ->
                    sendAction(HouseholdListAction.ShowMessage(error.message ?: "Неверный код"))
                }
        }
    }
}
