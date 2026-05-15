package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.fcm

import com.android.rut.miit.productinventory.domain.model.NotificationDeviceToken
import com.android.rut.miit.productinventory.domain.model.NotificationPlatform
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationDeviceTokenRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.util.Base64
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class FcmNotificationSenderTest {

    private val objectMapper = ObjectMapper().registerKotlinModule()

    @Test
    fun `send push skips safely when Firebase config is missing`() {
        val userId = UUID.randomUUID()
        val tokenRepository = InMemoryNotificationDeviceTokenRepository()
        val token = tokenRepository.upsert(userId, "token-1", NotificationPlatform.ANDROID)
        val sender = sender(tokenRepository = tokenRepository, projectId = "", serviceAccountJson = "")

        sender.sendPush(userId, "Low stock", "Rice is low")

        assertTrue(tokenRepository.findById(token.id)?.active == true)
    }

    @Test
    fun `send push skips safely when Firebase service account path is invalid`() {
        val userId = UUID.randomUUID()
        val tokenRepository = InMemoryNotificationDeviceTokenRepository()
        val token = tokenRepository.upsert(userId, "token-1", NotificationPlatform.ANDROID)
        val sender = sender(
            tokenRepository = tokenRepository,
            projectId = "project-1",
            serviceAccountJson = "",
            serviceAccountPath = "/path/does/not/exist/firebase.json"
        )

        sender.sendPush(userId, "Low stock", "Rice is low")

        assertTrue(tokenRepository.findById(token.id)?.active == true)
    }

    @Test
    fun `send push deactivates token after Firebase not found response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val userId = UUID.randomUUID()
        val tokenRepository = InMemoryNotificationDeviceTokenRepository()
        val token = tokenRepository.upsert(userId, "token-1", NotificationPlatform.ANDROID)
        val sender = sender(
            tokenRepository = tokenRepository,
            builder = builder,
            projectId = "project-1",
            serviceAccountJson = serviceAccountJson(tokenUri = "https://oauth.test/token")
        )

        server.expect(requestTo("https://oauth.test/token"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withSuccess(
                    """{"access_token":"oauth-token","expires_in":3600}""",
                    MediaType.APPLICATION_JSON
                )
            )
        server.expect(requestTo("https://fcm.googleapis.com/v1/projects/project-1/messages:send"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer oauth-token"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        sender.sendPush(userId, "Low stock", "Rice is low")

        assertFalse(tokenRepository.findById(token.id)?.active == true)
        server.verify()
    }

    private fun sender(
        tokenRepository: InMemoryNotificationDeviceTokenRepository,
        builder: RestClient.Builder = RestClient.builder(),
        projectId: String,
        serviceAccountJson: String,
        serviceAccountPath: String = ""
    ): FcmNotificationSender =
        FcmNotificationSender(
            tokenRepository = tokenRepository,
            objectMapper = objectMapper,
            restClientBuilder = builder,
            projectId = projectId,
            serviceAccountJson = serviceAccountJson,
            serviceAccountPath = serviceAccountPath
        )

    private fun serviceAccountJson(tokenUri: String): String {
        val privateKey = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(2048) }
            .generateKeyPair()
            .private as RSAPrivateKey
        return objectMapper.writeValueAsString(
            mapOf(
                "client_email" to "firebase@example.test",
                "private_key" to privateKey.toPem(),
                "token_uri" to tokenUri
            )
        )
    }

    private fun RSAPrivateKey.toPem(): String {
        val encoded = Base64.getMimeEncoder(64, "\n".toByteArray())
            .encodeToString(this.encoded)
        return """
            -----BEGIN PRIVATE KEY-----
            $encoded
            -----END PRIVATE KEY-----
        """.trimIndent()
    }

    private class InMemoryNotificationDeviceTokenRepository : INotificationDeviceTokenRepository {
        private val tokensById = linkedMapOf<UUID, NotificationDeviceToken>()

        override fun findActiveByUserId(userId: UUID): List<NotificationDeviceToken> =
            tokensById.values.filter { it.userId == userId && it.active }

        override fun upsert(userId: UUID, token: String, platform: NotificationPlatform): NotificationDeviceToken {
            val existing = tokensById.values.firstOrNull { it.token == token }
            val saved = existing?.copy(userId = userId, platform = platform, active = true)
                ?: NotificationDeviceToken(userId = userId, token = token, platform = platform)
            tokensById[saved.id] = saved
            return saved
        }

        override fun deactivate(userId: UUID, tokenId: UUID) {
            val existing = tokensById[tokenId]?.takeIf { it.userId == userId } ?: return
            tokensById[tokenId] = existing.copy(active = false)
        }

        override fun deactivateByToken(token: String) {
            val existing = tokensById.values.firstOrNull { it.token == token } ?: return
            tokensById[existing.id] = existing.copy(active = false)
        }

        fun findById(id: UUID): NotificationDeviceToken? = tokensById[id]
    }
}
