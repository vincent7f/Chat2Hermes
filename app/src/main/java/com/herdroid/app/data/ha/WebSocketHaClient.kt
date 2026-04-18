package com.herdroid.app.data.ha

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * Default HA transport: WebSocket text frames. Replace with SSE/gRPC when HA protocol is defined.
 */
class WebSocketHaClient : HaClient {

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null

    private val _connectionState = MutableStateFlow<HaConnectionState>(HaConnectionState.Disconnected)
    override val connectionState: StateFlow<HaConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val messages: Flow<String> = _messages.asSharedFlow()

    override fun connect(webSocketUrl: String, bearerToken: String) {
        disconnect()
        _connectionState.value = HaConnectionState.Connecting
        val rb = Request.Builder().url(webSocketUrl)
        val key = bearerToken.trim()
        if (key.isNotEmpty()) {
            rb.header("Authorization", "Bearer $key")
        }
        val request = rb.build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = HaConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                _messages.tryEmit(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _messages.tryEmit(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (_connectionState.value != HaConnectionState.Connecting) {
                    _connectionState.value = HaConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = HaConnectionState.Error(t.message ?: t.javaClass.simpleName)
            }
        })
    }

    override fun disconnect() {
        socket?.cancel()
        socket = null
        _connectionState.value = HaConnectionState.Disconnected
    }
}
