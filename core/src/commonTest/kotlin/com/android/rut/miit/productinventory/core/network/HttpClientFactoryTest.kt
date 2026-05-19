package com.android.rut.miit.productinventory.core.network

import com.android.rut.miit.productinventory.core.storage.InMemoryTokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class HttpClientFactoryTest {

    @Test
    fun `bearer auth reads latest stored token for each api request`() = runTest {
        val tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens(accessToken = "first-token", refreshToken = "refresh-token")
        val authorizationHeaders = mutableListOf<String?>()
        val engine = MockEngine { request ->
            authorizationHeaders += request.headers[HttpHeaders.Authorization]
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClientFactory(tokenStorage).create(engine)

        client.get("${ApiConstants.API_V1}/first")
        tokenStorage.saveTokens(accessToken = "second-token", refreshToken = "refresh-token")
        client.get("${ApiConstants.API_V1}/second")
        client.get("http://example.com/third")

        assertEquals(
            listOf("Bearer first-token", "Bearer second-token", null),
            authorizationHeaders
        )
    }

    @Test
    fun `throws api exception with server error message for non success response`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"message":"Invalid credentials","code":"UNAUTHORIZED"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClientFactory(InMemoryTokenStorage()).create(engine)

        val error = assertFailsWith<ApiException> {
            client.get("${ApiConstants.API_V1}/auth/login").bodyAsText()
        }

        assertEquals(401, error.statusCode)
        assertEquals("Invalid credentials", error.message)
    }

    @Test
    fun `refresh network failure does not clear stored offline session`() = runTest {
        val tokenStorage = InMemoryTokenStorage()
        tokenStorage.saveTokens(accessToken = "expired-token", refreshToken = "refresh-token")
        val engine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/api/v1/protected" -> respond(
                    content = """{"message":"Expired","code":"UNAUTHORIZED"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/api/v1/auth/refresh" -> error("offline")
                else -> respond(
                    content = "{}",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        }
        val client = HttpClientFactory(tokenStorage).create(engine)

        assertFailsWith<ApiException> {
            client.get("${ApiConstants.API_V1}/protected").bodyAsText()
        }

        assertNotNull(tokenStorage.getAccessToken())
        assertNotNull(tokenStorage.getRefreshToken())
    }
}
