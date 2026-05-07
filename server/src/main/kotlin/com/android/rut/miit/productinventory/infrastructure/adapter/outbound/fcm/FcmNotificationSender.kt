package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.fcm

import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FcmNotificationSender : INotificationSender {

    private val log = LoggerFactory.getLogger(FcmNotificationSender::class.java)

    override fun sendPush(userId: UUID, title: String, message: String) {
        // TODO: integrate with Firebase Cloud Messaging
        log.info("FCM push [user=$userId]: $title — $message")
    }
}
