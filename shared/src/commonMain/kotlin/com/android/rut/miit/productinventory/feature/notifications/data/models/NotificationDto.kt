package com.android.rut.miit.productinventory.feature.notifications.data.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationResponseDto(
    val id: String,
    val title: String,
    val message: String,
    val sentAt: String,
    val isRead: Boolean
)
