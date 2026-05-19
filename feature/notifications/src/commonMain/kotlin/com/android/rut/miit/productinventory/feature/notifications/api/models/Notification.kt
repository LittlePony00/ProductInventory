package com.android.rut.miit.productinventory.feature.notifications.api.models

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val sentAt: String,
    val isRead: Boolean
)
