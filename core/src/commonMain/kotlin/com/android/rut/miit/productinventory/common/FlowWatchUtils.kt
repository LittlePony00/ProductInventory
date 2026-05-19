package com.android.rut.miit.productinventory.common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.DisposableHandle

/**
 * Helper for iOS: subscribe to Flow on main thread and get handle for dispose().
 *
 * Usage in Swift:
 *   let handle = vm.viewState.watch { state in ... }
 *   handle.dispose()
 */
fun <T> Flow<T>.watch(onEach: (T) -> Unit): DisposableHandle {
    val scope = MainScope()
    val job = scope.launch(Dispatchers.Main.immediate) {
        this@watch
            .onCompletion { /* no-op */ }
            .collect { onEach(it) }
    }
    return DisposableHandle { job.cancel() }
}

/**
 * Returns single DisposableHandle for both state and action (both subscriptions will close together).
 */
fun <S : Any, A : Any> bind(
    state: Flow<S>,
    onState: (S) -> Unit,
    action: Flow<A>,
    onAction: (A) -> Unit
): DisposableHandle {
    val s = state.watch(onState)
    val a = action.watch(onAction)
    return DisposableHandle {
        s.dispose()
        a.dispose()
    }
}
