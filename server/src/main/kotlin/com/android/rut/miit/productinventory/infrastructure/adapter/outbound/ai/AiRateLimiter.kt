package com.android.rut.miit.productinventory.infrastructure.adapter.outbound.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class AiRateLimiter(
    @param:Value("\${gigachat.requests-per-minute:30}") private val requestsPerMinute: Int
) {
    private val windowStart = AtomicReference(Instant.EPOCH)
    private val requestCount = AtomicInteger(0)
    private val clock: Clock = Clock.systemUTC()

    fun tryAcquire(): Boolean {
        if (requestsPerMinute <= 0) return false
        val now = clock.instant()
        resetWindowIfNeeded(now)
        return requestCount.incrementAndGet() <= requestsPerMinute
    }

    private fun resetWindowIfNeeded(now: Instant) {
        val currentStart = windowStart.get()
        if (currentStart.plusSeconds(WINDOW_SECONDS).isAfter(now)) return
        if (windowStart.compareAndSet(currentStart, now)) {
            requestCount.set(0)
        }
    }

    private companion object {
        const val WINDOW_SECONDS = 60L
    }
}
