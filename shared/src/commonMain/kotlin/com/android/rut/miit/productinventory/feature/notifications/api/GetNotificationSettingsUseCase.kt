package com.android.rut.miit.productinventory.feature.notifications.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings

class GetNotificationSettingsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(): NotificationSettings = repository.getSettings()
}
