package com.herdroid.app.data.ha

sealed interface HaConnectionState {
    data object Disconnected : HaConnectionState
    data object Connecting : HaConnectionState
    data object Connected : HaConnectionState
    data class Error(val message: String) : HaConnectionState
}
