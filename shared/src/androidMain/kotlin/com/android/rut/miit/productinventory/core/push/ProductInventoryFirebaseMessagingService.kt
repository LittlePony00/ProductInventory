package com.android.rut.miit.productinventory.core.push

import com.google.firebase.messaging.FirebaseMessagingService
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
}
