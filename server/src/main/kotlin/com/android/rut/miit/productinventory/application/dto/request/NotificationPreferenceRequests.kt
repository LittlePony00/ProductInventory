package com.android.rut.miit.productinventory.application.dto.request

import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateNotificationSettingsRequest(
    val expirationRemindersEnabled: Boolean? = null,
    val lowStockRemindersEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null,
    val expirationReminderDays: Int? = null
)

data class RegisterDeviceTokenRequest(
    @field:NotBlank(message = "Device token is required")
    val token: String,

    @field:NotNull(message = "Platform is required")
    val platform: NotificationPlatform
)
