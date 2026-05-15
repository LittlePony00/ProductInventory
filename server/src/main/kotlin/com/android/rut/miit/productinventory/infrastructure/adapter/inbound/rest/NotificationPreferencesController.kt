package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.RegisterDeviceTokenRequest
import com.android.rut.miit.productinventory.application.dto.request.UpdateNotificationSettingsRequest
import com.android.rut.miit.productinventory.application.dto.response.NotificationDeviceTokenResponse
import com.android.rut.miit.productinventory.application.dto.response.NotificationSettingsResponse
import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationSettings
import com.android.rut.miit.productinventory.domain.port.inbound.INotificationPreferencesService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications/preferences")
class NotificationPreferencesController(
    private val preferencesService: INotificationPreferencesService
) {
    @GetMapping
    fun getSettings(): NotificationSettingsResponse =
        preferencesService.getSettings(currentUserId()).toResponse()

    @PutMapping
    fun updateSettings(@RequestBody request: UpdateNotificationSettingsRequest): NotificationSettingsResponse =
        preferencesService.updateSettings(
            userId = currentUserId(),
            expirationRemindersEnabled = request.expirationRemindersEnabled,
            lowStockRemindersEnabled = request.lowStockRemindersEnabled,
            pushEnabled = request.pushEnabled,
            expirationReminderDays = request.expirationReminderDays
        ).toResponse()

    @PostMapping("/device-tokens")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerDeviceToken(
        @Valid @RequestBody request: RegisterDeviceTokenRequest
    ): NotificationDeviceTokenResponse =
        preferencesService.registerDeviceToken(currentUserId(), request.token, request.platform).toResponse()

    @DeleteMapping("/device-tokens/{tokenId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deactivateDeviceToken(@PathVariable tokenId: UUID) {
        preferencesService.deactivateDeviceToken(currentUserId(), tokenId)
    }
}

private fun NotificationSettings.toResponse() = NotificationSettingsResponse(
    expirationRemindersEnabled = expirationRemindersEnabled,
    lowStockRemindersEnabled = lowStockRemindersEnabled,
    pushEnabled = pushEnabled,
    expirationReminderDays = expirationReminderDays,
    updatedAt = updatedAt
)

private fun NotificationDeviceToken.toResponse() = NotificationDeviceTokenResponse(
    id = id,
    platform = platform,
    active = active,
    createdAt = createdAt,
    lastSeenAt = lastSeenAt
)
