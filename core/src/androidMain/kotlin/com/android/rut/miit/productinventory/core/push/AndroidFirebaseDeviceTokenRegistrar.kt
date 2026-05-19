package com.android.rut.miit.productinventory.core.push

import android.content.Context
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
    private val tokenStorage: TokenStorage,
    context: Context
) : DeviceTokenRegistrar {
    private val preferences = context.applicationContext.getSharedPreferences(
        DEVICE_TOKEN_REGISTRATION_PREFERENCES,
        Context.MODE_PRIVATE
    )

    override suspend fun registerCurrentToken() {
        val token = runCatching { currentFirebaseToken() }.getOrNull()
            ?: pendingToken()
            ?: return
        registerToken(token)
    }

    override suspend fun registerToken(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) return
        savePendingToken(normalizedToken)
        if (tokenStorage.getAccessToken().isNullOrBlank()) return
        httpClient.post("${ApiConstants.API_V1}/notifications/preferences/device-tokens") {
            setBody(RegisterDeviceTokenRequestDto(token = normalizedToken, platform = "ANDROID"))
        }
        clearPendingToken(normalizedToken)
    }

    private suspend fun currentFirebaseToken(): String =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> continuation.resume(token) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }

    private fun savePendingToken(token: String) {
        preferences.edit().putString(PENDING_DEVICE_TOKEN_KEY, token).apply()
    }

    private fun pendingToken(): String? =
        preferences.getString(PENDING_DEVICE_TOKEN_KEY, null)?.takeIf { it.isNotBlank() }

    private fun clearPendingToken(token: String) {
        if (pendingToken() == token) {
            preferences.edit().remove(PENDING_DEVICE_TOKEN_KEY).apply()
        }
    }
}

@Serializable
private data class RegisterDeviceTokenRequestDto(
    val token: String,
    val platform: String
)

private const val DEVICE_TOKEN_REGISTRATION_PREFERENCES = "device_token_registration"
private const val PENDING_DEVICE_TOKEN_KEY = "pending_device_token"
