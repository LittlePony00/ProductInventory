package com.android.rut.miit.productinventory.feature.notifications.data

import com.android.rut.miit.productinventory.feature.notifications.api.NotificationRepository
import com.android.rut.miit.productinventory.feature.notifications.api.models.Notification
import com.android.rut.miit.productinventory.feature.notifications.data.mappers.toDomain

class NotificationRepositoryImpl(
    private val remoteDataSource: NotificationRemoteDataSource
) : NotificationRepository {

    override suspend fun getNotifications(): List<Notification> {
        return remoteDataSource.getNotifications().map { it.toDomain() }
    }

    override suspend fun getUnreadNotifications(): List<Notification> {
        return remoteDataSource.getUnreadNotifications().map { it.toDomain() }
    }

    override suspend fun markAsRead(notificationId: String) {
        remoteDataSource.markAsRead(notificationId)
    }

    override suspend fun markAllAsRead() {
        remoteDataSource.markAllAsRead()
    }
}
