package com.android.rut.miit.productinventory.feature.profile.presentation

import com.android.rut.miit.productinventory.common.UiAction

sealed class ProfileAction : UiAction {
    data class ShowMessage(val message: String) : ProfileAction()
    data object NavigateToLogin : ProfileAction()
    data object NavigateBack : ProfileAction()
}
