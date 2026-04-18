package com.herdroid.app.data.ha

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface HaClient {
    val connectionState: StateFlow<HaConnectionState>
    val messages: Flow<String>
    fun connect(webSocketUrl: String, bearerToken: String = "")
    fun disconnect()
}
