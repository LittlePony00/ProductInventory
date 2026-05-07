package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.response.NotificationResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.INotificationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationService: INotificationService
) {

    @GetMapping
    fun getNotifications(): List<NotificationResponse> {
        return notificationService.getNotifications(currentUserId()).map { it.toResponse() }
    }

    @GetMapping("/unread")
    fun getUnreadNotifications(): List<NotificationResponse> {
        return notificationService.getUnreadNotifications(currentUserId()).map { it.toResponse() }
    }

    @PutMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAsRead(@PathVariable notificationId: UUID) {
        notificationService.markAsRead(currentUserId(), notificationId)
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllAsRead() {
        notificationService.markAllAsRead(currentUserId())
    }
}
