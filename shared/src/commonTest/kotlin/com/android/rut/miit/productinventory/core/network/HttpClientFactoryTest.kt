package com.android.rut.miit.productinventory.core.network

import com.android.rut.miit.productinventory.core.storage.InMemoryTokenStorage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
