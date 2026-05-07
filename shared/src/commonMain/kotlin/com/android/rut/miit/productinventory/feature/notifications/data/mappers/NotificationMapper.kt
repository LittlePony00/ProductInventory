package com.android.rut.miit.productinventory.feature.notifications.data.mappers

import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.data.models.NotificationResponseDto

fun NotificationResponseDto.toDomain() = Notification(
    id = id,
    title = title,
    message = message,
    sentAt = sentAt,
    isRead = isRead
)
