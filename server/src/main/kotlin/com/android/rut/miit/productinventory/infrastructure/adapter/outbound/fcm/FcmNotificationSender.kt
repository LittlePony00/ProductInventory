package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.fcm

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationDeviceTokenRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class FcmNotificationSender(
    private val tokenRepository: INotificationDeviceTokenRepository,
    private val objectMapper: ObjectMapper,
    restClientBuilder: RestClient.Builder,
    @param:Value("\${firebase.project-id:}") private val projectId: String,
    @param:Value("\${firebase.service-account-json:}") private val serviceAccountJson: String,
    @param:Value("\${firebase.service-account-path:}") private val serviceAccountPath: String
) : INotificationSender {

    private val log = LoggerFactory.getLogger(FcmNotificationSender::class.java)
    private val restClient = restClientBuilder.build()
    private val cachedAccessToken = AtomicReference<FirebaseAccessToken?>(null)

    override fun sendPush(userId: UUID, title: String, message: String) {
        val tokens = tokenRepository.findActiveByUserId(userId)
        if (tokens.isEmpty()) {
            log.debug("FCM push skipped: no active device tokens for user={}", userId)
            return
        }
        val credentials = runCatching { loadServiceAccount() }
            .getOrElse { exception ->
                log.warn("FCM push skipped: Firebase credentials are invalid: {}", exception.message)
                return
            }
        if (projectId.isBlank() || credentials == null) {
            log.info("FCM push skipped: Firebase credentials are not configured [user={}, tokens={}]", userId, tokens.size)
            return
        }
        val accessToken = getAccessToken(credentials) ?: return
        tokens.forEach { token ->
            sendToToken(token, title, message, accessToken)
        }
    }

    private fun sendToToken(
        token: NotificationDeviceToken,
        title: String,
        message: String,
        accessToken: String
    ) {
        try {
            restClient.post()
                .uri("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                .header("Authorization", "Bearer $accessToken")
                .contentType(MediaType.APPLICATION_JSON)
                .body(FcmSendRequest.from(token.token, title, message))
                .retrieve()
                .toBodilessEntity()
            log.debug("FCM push sent [user={}, tokenId={}]", token.userId, token.id)
        } catch (exception: HttpClientErrorException) {
            if (exception.statusCode.value() in setOf(400, 404)) {
                tokenRepository.deactivateByToken(token.token)
                log.warn("FCM token deactivated after client error [tokenId={}, status={}]", token.id, exception.statusCode)
            } else {
                log.warn("FCM push failed [tokenId={}, status={}]: {}", token.id, exception.statusCode, exception.message)
            }
        } catch (exception: RuntimeException) {
            log.warn("FCM push failed [tokenId={}]: {}", token.id, exception.message)
        }
    }

    private fun getAccessToken(credentials: FirebaseServiceAccount): String? {
        cachedAccessToken.get()?.takeIf { it.expiresAt.isAfter(Instant.now().plusSeconds(60)) }?.let {
            return it.token
        }

        return try {
            val form = LinkedMultiValueMap<String, String>()
            form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            form.add("assertion", credentials.createJwtAssertion(objectMapper))

            val response = restClient.post()
                .uri(credentials.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(FirebaseTokenResponse::class.java)
                ?: return null
            val token = FirebaseAccessToken(
                token = response.accessToken,
                expiresAt = Instant.now().plusSeconds(response.expiresIn)
            )
            cachedAccessToken.set(token)
            token.token
        } catch (exception: RuntimeException) {
            log.warn("FCM OAuth token request failed: {}", exception.message)
            null
        }
    }

    private fun loadServiceAccount(): FirebaseServiceAccount? {
        val json = when {
            serviceAccountJson.isNotBlank() -> serviceAccountJson
            serviceAccountPath.isNotBlank() -> Files.readString(Path.of(serviceAccountPath))
            else -> return null
        }
        return objectMapper.readValue(json, FirebaseServiceAccount::class.java)
    }
}

private data class FcmSendRequest(
    val message: FcmMessage
) {
    companion object {
        fun from(token: String, title: String, body: String): FcmSendRequest =
            FcmSendRequest(
                FcmMessage(
                    token = token,
                    notification = FcmNotification(title = title, body = body)
                )
            )
    }
}

private data class FcmMessage(
    val token: String,
    val notification: FcmNotification
)

private data class FcmNotification(
    val title: String,
    val body: String
)

private data class FirebaseAccessToken(
    val token: String,
    val expiresAt: Instant
)

private data class FirebaseTokenResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,
    @param:JsonProperty("expires_in")
    val expiresIn: Long
)

private data class FirebaseServiceAccount(
    @param:JsonProperty("client_email")
    val clientEmail: String,
    @param:JsonProperty("private_key")
    val privateKey: String,
    @param:JsonProperty("token_uri")
    val tokenUri: String = "https://oauth2.googleapis.com/token"
) {
    fun createJwtAssertion(objectMapper: ObjectMapper): String {
        val now = Instant.now()
        val header = mapOf("alg" to "RS256", "typ" to "JWT")
        val claims = mapOf(
            "iss" to clientEmail,
            "scope" to "https://www.googleapis.com/auth/firebase.messaging",
            "aud" to tokenUri,
            "iat" to now.epochSecond,
            "exp" to now.plusSeconds(3600).epochSecond
        )
        val unsigned = listOf(header, claims)
            .joinToString(".") { objectMapper.writeValueAsBytes(it).base64Url() }
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(parsePrivateKey())
        signature.update(unsigned.toByteArray(Charsets.UTF_8))
        return "$unsigned.${signature.sign().base64Url()}"
    }

    private fun parsePrivateKey(): RSAPrivateKey {
        val pem = privateKey
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keySpec = PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem))
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec) as RSAPrivateKey
    }
}

private fun ByteArray.base64Url(): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(this)
