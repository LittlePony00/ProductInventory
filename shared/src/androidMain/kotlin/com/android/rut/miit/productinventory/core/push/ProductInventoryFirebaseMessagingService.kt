package com.android.rut.miit.productinventory.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class ProductInventoryFirebaseMessagingService : FirebaseMessagingService() {

    private val registrar: DeviceTokenRegistrar by lazy { GlobalContext.get().get() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { registrar.registerToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body
            ?: message.data["body"]
            ?: message.data["message"]
            ?: return

        showProductInventoryNotification(
            notificationId = message.messageId?.hashCode() ?: body.hashCode(),
            title = title,
            message = body,
            backendNotificationId = message.data["notificationId"]
        )
    }
}
