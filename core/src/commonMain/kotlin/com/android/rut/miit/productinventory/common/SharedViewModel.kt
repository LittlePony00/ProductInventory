package com.android.rut.miit.productinventory.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Base KMM ViewModel for presentation layer.
 * Designed according to MVI+UDF pattern.
 *
 * Contains three data streams:
 *  - [viewState]  — screen state. External access is read-only.
 *  - [viewAction] — one-time actions (navigation, messages, dialogs etc.; read-only).
 *  - incoming events from UI are accepted only through [onEvent]; no direct access to event stream.
 *
 * Encapsulation invariants:
 *  - External access allowed only: read [viewState]/[viewAction] and call [onEvent].
 *  - State changes/action generation only inside through [updateState] and [sendAction].
 *
 * @param S state type (usually data class).
 * @param E incoming event type (intent) from UI.
 * @param A action type for UI.
 * @param initialState starting state of viewmodel.
 */
abstract class SharedViewModel<S : UiState, E : UiEvent, A : UiAction>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val viewState: StateFlow<S> = _state.asStateFlow()

    private val _actions = MutableSharedFlow<A>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val viewAction: SharedFlow<A> = _actions.asSharedFlow()

    private val _events = MutableSharedFlow<E>(
        replay = 0,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    init {
        viewModelScope.launch {
            _events.collect { handleEvent(it) }
        }
    }

    /**
     * Handle for emitting events from UI into stream.
     * Event goes to handler coroutine [handleEvent].
     */
    fun onEvent(event: E) {
        viewModelScope.launch { _events.emit(event) }
    }

    /**
     * Change/update ViewState.
     * Used only inside the viewmodel itself.
     */
    protected fun updateState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    /**
     * Send ViewAction to UI.
     * Use for navigation/messages/dialogs.
     */
    protected fun sendAction(action: A) {
        _actions.tryEmit(action)
    }

    /**
     * Current state (for reading inside VM).
     */
    protected val currentState: S get() = _state.value

    /**
     * UI event handling. Here we change state and send actions.
     * Or call handlers - delegates, passing ViewEvent to them.
     */
    protected abstract suspend fun handleEvent(event: E)
}
