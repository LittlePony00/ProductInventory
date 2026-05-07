package com.android.rut.miit.productinventory.feature.notifications.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.notifications.data.models.NotificationResponseDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class NotificationRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getNotifications(): List<NotificationResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/notifications").body()
    }

    suspend fun getUnreadNotifications(): List<NotificationResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/notifications/unread").body()
    }

    suspend fun markAsRead(notificationId: String) {
        httpClient.put("${ApiConstants.API_V1}/notifications/$notificationId/read")
    }

    suspend fun markAllAsRead() {
        httpClient.put("${ApiConstants.API_V1}/notifications/read-all")
    }
}
