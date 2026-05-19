package com.android.rut.miit.productinventory.core.push

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidFirebaseDeviceTokenRegistrar(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : DeviceTokenRegistrar {

    override suspend fun registerCurrentToken() {
        val token = currentFirebaseToken()
        registerToken(token)
    }

    override suspend fun registerToken(token: String) {
        if (token.isBlank() || tokenStorage.getAccessToken().isNullOrBlank()) return
        httpClient.post("${ApiConstants.API_V1}/notifications/preferences/device-tokens") {
            setBody(RegisterDeviceTokenRequestDto(token = token, platform = "ANDROID"))
        }
    }

    private suspend fun currentFirebaseToken(): String =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> continuation.resume(token) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
}

@Serializable
private data class RegisterDeviceTokenRequestDto(
    val token: String,
    val platform: String
)
