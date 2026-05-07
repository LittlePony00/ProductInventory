package com.android.rut.miit.productinventory.feature.notifications.api

class MarkAllReadUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke() = repository.markAllAsRead()
}
