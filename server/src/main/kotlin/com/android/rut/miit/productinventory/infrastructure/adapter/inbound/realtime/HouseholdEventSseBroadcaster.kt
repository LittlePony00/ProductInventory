package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Component
class HouseholdEventSseBroadcaster(
    private val emitterFactory: SseEmitterFactory
) {
    private val subscriptions = ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>>()
    private val eventHistory = ConcurrentHashMap<UUID, ArrayDeque<HouseholdEvent>>()

    fun subscribe(householdId: UUID, lastEventId: UUID? = null): SseEmitter {
        val emitter = emitterFactory.create()
        subscriptions.computeIfAbsent(householdId) { CopyOnWriteArrayList() }.add(emitter)

        emitter.onCompletion { remove(householdId, emitter) }
        emitter.onTimeout { remove(householdId, emitter) }
        emitter.onError { remove(householdId, emitter) }

        replayMissedEvents(householdId, lastEventId, emitter)

        return emitter
    }

    fun publish(event: HouseholdEvent) {
        remember(event)
        val emitters = subscriptions[event.householdId].orEmpty()
        emitters.forEach { emitter ->
            send(event, emitter)
        }
    }

    fun missedEvents(householdId: UUID, lastEventId: UUID?): List<HouseholdEvent> =
        historyFor(householdId).let { history ->
            synchronized(history) {
                eventsAfter(history, lastEventId)
            }
        }

    private fun replayMissedEvents(householdId: UUID, lastEventId: UUID?, emitter: SseEmitter) {
        missedEvents(householdId, lastEventId).forEach { event ->
            send(event, emitter)
        }
    }

    private fun send(event: HouseholdEvent, emitter: SseEmitter) {
        try {
            emitter.send(
                SseEmitter.event()
                    .id(event.id.toString())
                    .name(event.type.name)
                    .data(event)
            )
        } catch (exception: IOException) {
            handleDeadEmitter(event.householdId, emitter, exception)
        } catch (exception: IllegalStateException) {
            handleDeadEmitter(event.householdId, emitter, exception)
        }
    }

    private fun handleDeadEmitter(householdId: UUID, emitter: SseEmitter, exception: Exception) {
        logger.warn("Removing dead SSE emitter for household {}", householdId, exception)
        remove(householdId, emitter)
    }

    private fun remember(event: HouseholdEvent) {
        val history = historyFor(event.householdId)
        synchronized(history) {
            history.addLast(event)
            while (history.size > HISTORY_LIMIT) {
                history.removeFirst()
            }
        }
    }

    private fun historyFor(householdId: UUID): ArrayDeque<HouseholdEvent> =
        eventHistory.computeIfAbsent(householdId) { ArrayDeque() }

    private fun eventsAfter(history: ArrayDeque<HouseholdEvent>, lastEventId: UUID?): List<HouseholdEvent> {
        if (lastEventId == null) return emptyList()
        val events = history.toList()
        val lastSeenIndex = events.indexOfFirst { it.id == lastEventId }
        return if (lastSeenIndex == -1) events else events.drop(lastSeenIndex + 1)
    }

    private fun remove(householdId: UUID, emitter: SseEmitter) {
        subscriptions[householdId]?.let { emitters ->
            emitters.remove(emitter)
            if (emitters.isEmpty()) {
                subscriptions.remove(householdId, emitters)
            }
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 512
        val logger = LoggerFactory.getLogger(HouseholdEventSseBroadcaster::class.java)
    }
}

fun interface SseEmitterFactory {
    fun create(): SseEmitter
}

@Component
class DefaultSseEmitterFactory : SseEmitterFactory {
    override fun create(): SseEmitter = SseEmitter(0L)
}
