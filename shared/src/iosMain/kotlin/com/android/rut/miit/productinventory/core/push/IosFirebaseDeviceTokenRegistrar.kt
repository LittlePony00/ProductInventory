package com.android.rut.miit.productinventory.core.push

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
import platform.Foundation.NSUserDefaults

class IosFirebaseDeviceTokenRegistrar(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) : DeviceTokenRegistrar {

    override suspend fun registerCurrentToken() {
        val token = IosPushTokenBridge.currentToken() ?: return
        registerToken(token)
    }

    override suspend fun registerToken(token: String) {
        val normalizedToken = token.trim()
        if (normalizedToken.isBlank()) return

        IosPushTokenBridge.setCurrentToken(normalizedToken)
        if (tokenStorage.getAccessToken().isNullOrBlank()) return

        httpClient.post("${ApiConstants.API_V1}/notifications/preferences/device-tokens") {
            setBody(RegisterDeviceTokenRequestDto(token = normalizedToken, platform = "IOS"))
        }
    }
}

object IosPushTokenBridge {
    private const val TOKEN_KEY = "productinventory.ios.fcm.token"

    fun setCurrentToken(token: String) {
        val normalizedToken = token.trim().takeIf { it.isNotBlank() } ?: return
        NSUserDefaults.standardUserDefaults.setObject(normalizedToken, TOKEN_KEY)
    }

    fun currentToken(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(TOKEN_KEY)
}

@Serializable
private data class RegisterDeviceTokenRequestDto(
    val token: String,
    val platform: String
)
