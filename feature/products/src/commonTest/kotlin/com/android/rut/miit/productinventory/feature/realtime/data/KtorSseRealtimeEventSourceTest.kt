package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class KtorSseRealtimeEventSourceTest {

    @Test
    fun `polls missed events with initial event id and deduplicates fallback events`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val engine = MockEngine { request ->
            requests += request
            when (request.url.encodedPath) {
                "/api/v1/households/h1/events/missed" -> respond(
                    content = """
                        [
                          {
                            "id": "event-1",
                            "type": "PRODUCT_CREATED",
                            "householdId": "h1",
                            "occurredAt": "2026-05-15T00:00:00Z",
                            "productId": "p1"
                          },
                          {
                            "id": "event-1",
                            "type": "PRODUCT_CREATED",
                            "householdId": "h1",
                            "occurredAt": "2026-05-15T00:00:00Z",
                            "productId": "p1"
                          },
                          {
                            "id": "event-2",
                            "type": "PRODUCT_DELETED",
                            "householdId": "h1",
                            "occurredAt": "2026-05-15T00:00:01Z",
                            "productId": "p1"
                          }
                        ]
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
                "/api/v1/households/h1/events" -> respondError(HttpStatusCode.ServiceUnavailable)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }

        val events = KtorSseRealtimeEventSource(httpClient(engine))
            .observeHouseholdEvents("h1")
            .take(2)
            .toList()

        assertEquals(listOf("event-1", "event-2"), events.map { it.id })
        val firstMissedRequest = requests.first {
            it.url.encodedPath == "/api/v1/households/h1/events/missed"
        }
        assertEquals(
            "00000000-0000-0000-0000-000000000000",
            firstMissedRequest.headers["Last-Event-ID"]
        )
        assertEquals("${ApiConstants.API_V1}/households/h1/events/missed", firstMissedRequest.url.toString())
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
