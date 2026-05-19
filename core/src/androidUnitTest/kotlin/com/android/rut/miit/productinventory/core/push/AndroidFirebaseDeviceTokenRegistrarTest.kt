package com.android.rut.miit.productinventory.core.push

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidFirebaseDeviceTokenRegistrarTest {

    @Test
    fun `register token without auth stores pending token and does not call backend`() = runTest {
        val context = testContext()
        val requests = mutableListOf<HttpRequestData>()
        val registrar = AndroidFirebaseDeviceTokenRegistrar(
            httpClient = httpClient(MockEngine { request ->
                requests += request
                respondOk()
            }),
            tokenStorage = FakeTokenStorage(),
            context = context
        )

        registrar.registerToken(" token-1 ")

        assertEquals("token-1", pendingToken(context))
        assertEquals(emptyList(), requests)
    }

    @Test
    fun `successful registration clears pending token`() = runTest {
        val context = testContext()
        val tokenStorage = FakeTokenStorage(accessToken = "access")
        val paths = mutableListOf<String>()
        val registrar = AndroidFirebaseDeviceTokenRegistrar(
            httpClient = httpClient(MockEngine { request ->
                paths += request.url.encodedPath
                respondOk()
            }),
            tokenStorage = tokenStorage,
            context = context
        )

        registrar.registerToken("token-1")

        assertEquals(listOf("/api/v1/notifications/preferences/device-tokens"), paths)
        assertNull(pendingToken(context))
    }

    @Test
    fun `failed registration keeps pending token for retry`() = runTest {
        val context = testContext()
        val registrar = AndroidFirebaseDeviceTokenRegistrar(
            httpClient = httpClient(MockEngine { error("offline") }),
            tokenStorage = FakeTokenStorage(accessToken = "access"),
            context = context
        )

        runCatching { registrar.registerToken("token-1") }

        assertEquals("token-1", pendingToken(context))
    }

    private fun testContext(): Context =
        ApplicationProvider.getApplicationContext<Context>().also {
            it.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().commit()
        }

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            defaultRequest {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }

    private fun MockRequestHandleScope.respondOk() =
        respond(
            content = "{}",
            status = HttpStatusCode.Created,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        )

    private fun pendingToken(context: Context): String? =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(PENDING_TOKEN_KEY, null)

    private class FakeTokenStorage(
        private var accessToken: String? = null
    ) : TokenStorage {
        override fun getAccessToken(): String? = accessToken
        override fun getRefreshToken(): String? = null
        override fun saveTokens(accessToken: String, refreshToken: String) {
            this.accessToken = accessToken
        }
        override fun clearTokens() {
            accessToken = null
        }
        override fun getUserId(): String? = null
        override fun saveUserId(userId: String) = Unit
    }

    private companion object {
        const val PREFERENCES = "device_token_registration"
        const val PENDING_TOKEN_KEY = "pending_device_token"
    }
}
