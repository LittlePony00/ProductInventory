package com.android.rut.miit.productinventory.feature.notifications.presentation

import com.android.rut.miit.productinventory.common.UiState
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings

sealed class NotificationListState : UiState {
    data object Loading : NotificationListState()
    data class Content(
        val notifications: List<Notification>,
        val settings: NotificationSettings,
        val isSavingSettings: Boolean = false,
        val settingsError: String? = null
    ) : NotificationListState()
    data class Error(val message: String?) : NotificationListState()
}
