package com.android.rut.miit.productinventory.feature.realtime.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.realtime.data.models.RealtimeEventDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readLine
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json

class AndroidSseRealtimeEventSource(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RealtimeEventSource {

    override fun observeHouseholdEvents(householdId: String): Flow<RealtimeEventDto> = flow {
        var retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
        var lastEventId: String? = null
        while (currentCoroutineContext().isActive) {
            runCatching {
                httpClient.prepareGet("${ApiConstants.API_V1}/households/$householdId/events") {
                    accept(ContentType.Text.EventStream)
                    lastEventId?.let { header("Last-Event-ID", it) }
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        throw IllegalStateException("Realtime stream failed: ${response.status}")
                    }
                    retryDelayMillis = INITIAL_RETRY_DELAY_MILLIS
                    val parser = SseMessageParser()
                    val channel = response.bodyAsChannel()
                    while (currentCoroutineContext().isActive && !channel.isClosedForRead) {
                        val line = channel.readLine() ?: break
                        parser.accept(line)?.let { message ->
                            val event = json.decodeFromString<RealtimeEventDto>(message.data)
                            lastEventId = event.id ?: message.id ?: lastEventId
                            emit(event)
                        }
                    }
                }
            }.onFailure { error ->
                if (error !is HttpRequestTimeoutException) {
                    delay(retryDelayMillis)
                    retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_RETRY_DELAY_MILLIS)
                }
            }
        }
    }

    private class SseMessageParser {
        private val dataLines = mutableListOf<String>()
        private var eventId: String? = null

        fun accept(line: String): SseMessage? {
            if (line.isBlank()) {
                val payload = dataLines.joinToString(separator = "\n")
                val id = eventId
                dataLines.clear()
                eventId = null
                return payload
                    .takeIf { it.isNotBlank() }
                    ?.let { SseMessage(id = id, data = it) }
            }
            if (line.startsWith("data:")) {
                dataLines += line.removePrefix("data:").trimStart()
            }
            if (line.startsWith("id:")) {
                eventId = line.removePrefix("id:").trim()
            }
            return null
        }
    }

    private data class SseMessage(
        val id: String?,
        val data: String
    )

    private companion object {
        const val INITIAL_RETRY_DELAY_MILLIS = 1_000L
        const val MAX_RETRY_DELAY_MILLIS = 30_000L
    }
}
