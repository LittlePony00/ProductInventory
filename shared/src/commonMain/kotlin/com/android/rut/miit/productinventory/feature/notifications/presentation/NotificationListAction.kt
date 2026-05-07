package com.android.rut.miit.productinventory.feature.notifications.presentation

import com.android.rut.miit.productinventory.common.UiAction

sealed class NotificationListAction : UiAction {
    data object NavigateBack : NotificationListAction()
}
