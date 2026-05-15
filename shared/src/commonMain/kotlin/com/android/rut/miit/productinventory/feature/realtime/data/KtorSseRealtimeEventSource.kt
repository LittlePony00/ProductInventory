package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class KtorSseRealtimeEventSource(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RealtimeEventSource {

    override fun observeHouseholdEvents(householdId: String): Flow<RealtimeEventDto> = channelFlow {
        var retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
        val cursor = RealtimeEventCursor()
        val lock = Mutex()

        suspend fun currentLastEventId(): String = lock.withLock { cursor.currentLastEventId }

        suspend fun acceptEvent(event: RealtimeEventDto): Boolean =
            lock.withLock { cursor.accept(event) }

        suspend fun sendIfNew(event: RealtimeEventDto) {
            if (acceptEvent(event)) {
                send(event)
            }
        }

        val missedEventPoller = launch {
            while (isActive) {
                runCatching {
                    fetchMissedEvents(householdId, currentLastEventId()).forEach { event ->
                        sendIfNew(event)
                    }
                }
                delay(MISSED_EVENT_POLL_INTERVAL_MILLIS)
            }
        }

        val streamObserver = launch {
            while (isActive) {
                runCatching {
                    val requestedLastEventId = currentLastEventId()
                    httpClient.sse(
                        urlString = "${ApiConstants.API_V1}/households/$householdId/events",
                        request = {
                            accept(ContentType.Text.EventStream)
                            header("Last-Event-ID", requestedLastEventId)
                        }
                    ) {
                        retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
                        incoming.collect { message ->
                            val data = message.data ?: return@collect
                            val event = json.decodeFromString<RealtimeEventDto>(data)
                            sendIfNew(event.copy(id = event.id ?: message.id))
                        }
                    }
                    if (isActive) {
                        delay(retryDelayMillis)
                    }
                }.onFailure { error ->
                    if (isActive && error !is HttpRequestTimeoutException) {
                        delay(retryDelayMillis)
                        retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
                    }
                }
            }
        }

        awaitClose {
            missedEventPoller.cancel()
            streamObserver.cancel()
        }
    }

    private suspend fun fetchMissedEvents(householdId: String, lastEventId: String): List<RealtimeEventDto> =
        httpClient.get("${ApiConstants.API_V1}/households/$householdId/events/missed") {
            header("Last-Event-ID", lastEventId)
        }.body()

    private companion object {
        const val INITIAL_LAST_EVENT_ID = "00000000-0000-0000-0000-000000000000"
        const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
        const val MAX_RETRY_DELAY_MILLIS = 30_000L
        const val MISSED_EVENT_POLL_INTERVAL_MILLIS = 3_000L
        const val SEEN_EVENT_LIMIT = 512
    }
}

internal class RealtimeEventCursor(
    private val seenEventLimit: Int = 512
) {
    private val seenEventIds = mutableSetOf<String>()
    private val seenEventOrder = ArrayDeque<String>()

    var currentLastEventId: String = "00000000-0000-0000-0000-000000000000"
        private set

    fun accept(event: RealtimeEventDto): Boolean {
        val eventId = event.id
        if (eventId != null && !seenEventIds.add(eventId)) {
            return false
        }
        if (eventId != null) {
            seenEventOrder.addLast(eventId)
            while (seenEventOrder.size > seenEventLimit) {
                seenEventIds.remove(seenEventOrder.removeFirst())
            }
            currentLastEventId = eventId
        }
        return true
    }
}
