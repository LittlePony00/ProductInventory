package com.android.rut.miit.productinventory.feature.notifications.presentation

import com.android.rut.miit.productinventory.common.UiEvent

sealed class NotificationListEvent : UiEvent {
    data object OnCreate : NotificationListEvent()
    data object OnRetry : NotificationListEvent()
    data class OnMarkRead(val notificationId: String) : NotificationListEvent()
    data object OnMarkAllRead : NotificationListEvent()
    data class OnExpirationRemindersEnabledChange(val enabled: Boolean) : NotificationListEvent()
    data class OnLowStockRemindersEnabledChange(val enabled: Boolean) : NotificationListEvent()
    data class OnPushEnabledChange(val enabled: Boolean) : NotificationListEvent()
    data class OnExpirationReminderDaysChange(val days: Int) : NotificationListEvent()
    data object OnBackClick : NotificationListEvent()
}
