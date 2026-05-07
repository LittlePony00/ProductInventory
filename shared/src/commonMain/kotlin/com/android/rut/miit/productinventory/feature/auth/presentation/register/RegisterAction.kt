package com.android.rut.miit.productinventory.feature.auth.presentation.register

import com.android.rut.miit.productinventory.common.UiAction

sealed class RegisterAction : UiAction {
    data object NavigateToHome : RegisterAction()
    data object NavigateBack : RegisterAction()
}
