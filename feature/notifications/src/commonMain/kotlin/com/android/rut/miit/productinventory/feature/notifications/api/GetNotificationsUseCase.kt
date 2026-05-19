package com.android.rut.miit.productinventory.feature.notifications.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification

class GetNotificationsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): List<Notification> = repository.getNotifications()
}
