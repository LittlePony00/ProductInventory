package com.android.rut.miit.productinventory.feature.notifications.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification

sealed class NotificationListState : UiState {
    data object Loading : NotificationListState()
    data class Content(val notifications: List<Notification>) : NotificationListState()
    data class Error(val message: String?) : NotificationListState()
}
