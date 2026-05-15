package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

@Component
class GigaChatAccessTokenProvider(
    restClientBuilder: RestClient.Builder,
    @param:Value("\${gigachat.api-key:}") private val apiKey: String,
    @param:Value("\${gigachat.oauth-url:https://ngw.devices.sberbank.ru:9443/api/v2/oauth}") private val oauthUrl: String,
    @param:Value("\${gigachat.scope:GIGACHAT_API_PERS}") private val scope: String
) {
    private val log = LoggerFactory.getLogger(GigaChatAccessTokenProvider::class.java)
    private val restClient = restClientBuilder.build()
    private val cachedAccessToken = AtomicReference<GigaChatAccessToken?>(null)

    fun getAccessToken(): String? {
        if (apiKey.isBlank()) return null
        cachedAccessToken.get()?.takeIf { it.expiresAt.isAfter(Instant.now().plusSeconds(60)) }?.let {
            return it.token
        }

        return try {
            val form = LinkedMultiValueMap<String, String>()
            form.add("scope", scope)
            val response = restClient.post()
                .uri(oauthUrl)
                .header("Authorization", normalizedAuthorizationHeader())
                .header("RqUID", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .body(form)
                .retrieve()
                .body(GigaChatOAuthResponse::class.java)
                ?: return null
            val token = GigaChatAccessToken(
                token = response.accessToken,
                expiresAt = response.expiresAt.toInstant()
            )
            cachedAccessToken.set(token)
            token.token
        } catch (exception: RestClientException) {
            log.warn("GigaChat OAuth token request failed: {}", exception.message)
            null
        } catch (exception: RuntimeException) {
            log.warn("GigaChat OAuth response is invalid: {}", exception.message)
            null
        }
    }

    private fun normalizedAuthorizationHeader(): String =
        when {
            apiKey.startsWith("Basic ", ignoreCase = true) -> apiKey
            else -> "Basic $apiKey"
        }
}

private data class GigaChatAccessToken(
    val token: String,
    val expiresAt: Instant
)

private data class GigaChatOAuthResponse(
    @param:JsonProperty("access_token")
    val accessToken: String,
    @param:JsonProperty("expires_at")
    val expiresAt: Long
)

private fun Long.toInstant(): Instant =
    if (this > 1_000_000_000_000L) {
        Instant.ofEpochMilli(this)
    } else {
        Instant.ofEpochSecond(this)
    }
