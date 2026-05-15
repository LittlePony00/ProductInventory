package com.android.rut.miit.productinventory.feature.notifications.api

import com.android.rut.miit.productinventory.feature.notifications.api.models.NotificationSettings

class UpdateNotificationSettingsUseCase(private val repository: NotificationRepository) {
    suspend operator fun invoke(
        expirationRemindersEnabled: Boolean?,
        lowStockRemindersEnabled: Boolean?,
        pushEnabled: Boolean?,
        expirationReminderDays: Int?
    ): NotificationSettings =
        repository.updateSettings(
            expirationRemindersEnabled = expirationRemindersEnabled,
            lowStockRemindersEnabled = lowStockRemindersEnabled,
            pushEnabled = pushEnabled,
            expirationReminderDays = expirationReminderDays
        )
}
