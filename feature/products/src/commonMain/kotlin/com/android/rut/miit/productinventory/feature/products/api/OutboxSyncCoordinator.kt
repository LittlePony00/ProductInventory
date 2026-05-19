package com.android.rut.miit.productinventory.feature.products.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OutboxSyncCoordinator(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository
) {
    private val mutex = Mutex()
    private val states = mutableMapOf<String, SyncState>()

    suspend fun sync(householdId: String) {
        val normalizedHouseholdId = householdId.takeIf { it.isNotBlank() } ?: return
        if (!markRunningOrRequestRerun(normalizedHouseholdId)) return

        while (true) {
            val failure = runCatching {
                categoryRepository.refreshCategories(normalizedHouseholdId)
                productRepository.refreshProducts(normalizedHouseholdId)
            }.exceptionOrNull()

            val shouldRunAgain = mutex.withLock {
                val state = states.getValue(normalizedHouseholdId)
                if (state.rerunRequested) {
                    state.rerunRequested = false
                    true
                } else {
                    state.running = false
                    false
                }
            }

            if (!shouldRunAgain) {
                failure?.let { throw it }
                return
            }
        }
    }

    private suspend fun markRunningOrRequestRerun(householdId: String): Boolean =
        mutex.withLock {
            val state = states.getOrPut(householdId) { SyncState() }
            if (state.running) {
                state.rerunRequested = true
                false
            } else {
                state.running = true
                true
            }
        }

    private class SyncState(
        var running: Boolean = false,
        var rerunRequested: Boolean = false
    )
}
