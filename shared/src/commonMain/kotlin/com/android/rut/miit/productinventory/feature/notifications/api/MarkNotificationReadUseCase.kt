package com.android.rut.miit.productinventory.feature.notifications.api

class MarkNotificationReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(notificationId: String) = repository.markAsRead(notificationId)
}
