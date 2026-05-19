package com.android.rut.miit.productinventory.feature.auth.presentation.login

import com.android.rut.miit.productinventory.common.UiAction

sealed class LoginAction : UiAction {
    data object NavigateToHome : LoginAction()
    data object NavigateToRegister : LoginAction()
}
