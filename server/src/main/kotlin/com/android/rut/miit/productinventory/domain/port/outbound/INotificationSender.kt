package com.android.rut.miit.productinventory.domain.port.outbound

import java.util.UUID

interface INotificationSender {
    fun sendPush(userId: UUID, title: String, message: String)
}
