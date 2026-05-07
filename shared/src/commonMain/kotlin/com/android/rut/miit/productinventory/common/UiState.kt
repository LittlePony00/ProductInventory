package com.android.rut.miit.productinventory.common

/**
 * Marker aliases for contract clarity.
 * May not be implemented explicitly, but convenient for type uniformity.
 */

/**
 * Marker for class intended to store current UI state
 */
interface UiState

/**
 * Marker for class intended to pass events from UI (View)
 * In MVI terminology - Intent
 */
interface UiEvent

/**
 * Marker for class intended to pass actions that ViewModel creates,
 * and native presentation executes. Often Router executes these actions in navigation
 */
interface UiAction
