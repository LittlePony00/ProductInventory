package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorSseRealtimeEventSourceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `does not poll missed events while observing sse stream`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v1/households/h1/events" -> respondError(HttpStatusCode.ServiceUnavailable)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val job = launch {
            KtorSseRealtimeEventSource(httpClient(engine))
                .observeHouseholdEvents("h1")
                .collect()
        }
        runCurrent()
        advanceTimeBy(3_500L)
        job.cancelAndJoin()

        assertTrue(requests.none { it.url.encodedPath == "/api/v1/households/h1/events/missed" })
    }

    @Test
    fun `reconnect cursor advances after stream event and deduplicates repeated replay`() {
        val cursor = RealtimeEventCursor()

        assertEquals("00000000-0000-0000-0000-000000000000", cursor.currentLastEventId)
        assertEquals(true, cursor.accept(event("sse-1")))
        assertEquals("sse-1", cursor.currentLastEventId)

        assertEquals(false, cursor.accept(event("sse-1")))
        assertEquals("sse-1", cursor.currentLastEventId)

        assertEquals(true, cursor.accept(event("sse-2")))
        assertEquals("sse-2", cursor.currentLastEventId)
    }

    private fun httpClient(engine: MockEngine): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(SSE)
        }

    private fun event(id: String): RealtimeEventDto = RealtimeEventDto(
        id = id,
        type = "PRODUCT_CREATED",
        householdId = "h1",
        occurredAt = "2026-05-15T00:00:00Z",
        productId = "p1"
    )
}
