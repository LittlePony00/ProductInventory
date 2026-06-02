package com.android.rut.miit.productinventory.core.network

import com.android.rut.miit.productinventory.core.storage.TokenStorage
import io.ktor.client.*
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.call.body
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpClientFactory(private val tokenStorage: TokenStorage) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    fun create(): HttpClient = HttpClient {
        configure()
    }

    internal fun create(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
        configure()
    }

    private fun HttpClientConfig<*>.configure() {
        install(ContentNegotiation) {
            json(json)
        }

        install(Logging) {
            level = LogLevel.BODY
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }

        install(SSE)

        install(Auth) {
            bearer {
                val apiUrl = Url(ApiConstants.BASE_URL)
                cacheTokens = false
                sendWithoutRequest { request ->
                    request.url.protocol == apiUrl.protocol &&
                        request.url.host == apiUrl.host &&
                        request.url.port == apiUrl.port
                }

                loadTokens {
                    val access = tokenStorage.getAccessToken()
                    val refresh = tokenStorage.getRefreshToken()
                    if (access != null && refresh != null) {
                        BearerTokens(access, refresh)
                    } else null
                }

                refreshTokens {
                    val refresh = tokenStorage.getRefreshToken() ?: return@refreshTokens null
                    val response = runCatching {
                        client.post("${ApiConstants.API_V1}/auth/refresh") {
                            contentType(ContentType.Application.Json)
                            setBody(mapOf("refreshToken" to refresh))
                            markAsRefreshTokenRequest()
                        }
                    }.getOrElse { error ->
                        if (error.isAuthenticationRejected()) {
                            tokenStorage.clearTokens()
                        }
                        return@refreshTokens null
                    }

                    if (response.status == HttpStatusCode.OK) {
                        val body = response.body<TokenRefreshResponse>()
                        tokenStorage.saveTokens(body.accessToken, body.refreshToken)
                        BearerTokens(body.accessToken, body.refreshToken)
                    } else {
                        tokenStorage.clearTokens()
                        null
                    }
                }
            }
        }

        HttpResponseValidator {
            validateResponse { response ->
                val statusCode = response.status.value
                if (statusCode < 400) return@validateResponse

                val message = runCatching {
                    json.decodeFromString<ErrorResponseDto>(response.bodyAsText()).message
                }.getOrNull()
                    ?: response.status.description

                throw ApiException(statusCode, message)
            }
        }
    }
}

class ApiException(
    val statusCode: Int,
    override val message: String
) : RuntimeException(message)

private fun Throwable.isAuthenticationRejected(): Boolean =
    this is ApiException && statusCode in setOf(401, 403)

@Serializable
private data class ErrorResponseDto(
    val message: String? = null,
    val code: String? = null
)

@Serializable
internal data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String
)
