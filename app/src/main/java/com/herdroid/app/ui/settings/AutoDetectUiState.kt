package com.herdroid.app.ui.settings

/**
 * UI state for Hermes WebSocket auto-detect flow.
 */
sealed interface AutoDetectUiState {
    data object Idle : AutoDetectUiState

    /** Scanning [currentIndex] of [total] combinations; currently probing [scheme] + [port]. */
    data class Scanning(
        val currentIndex: Int,
        val total: Int,
        val scheme: String,
        val port: Int,
    ) : AutoDetectUiState

    /** A working endpoint was found; waiting for user to apply or skip. */
    data class AskingUser(
        val scheme: String,
        val port: Int,
    ) : AutoDetectUiState
}
