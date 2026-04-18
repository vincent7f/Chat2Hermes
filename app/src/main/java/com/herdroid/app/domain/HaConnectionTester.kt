package com.herdroid.app.domain

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.coroutines.resume

/**
 * Short-lived WebSocket open test (matches [com.herdroid.app.data.ha.WebSocketHaClient] transport).
 */
object HaConnectionTester {

    suspend fun testWebSocket(
        client: OkHttpClient,
        url: String,
        timeoutMs: Long = 12_000,
    ): Result<Unit> {
        return try {
            withTimeout(timeoutMs) {
                suspendCancellableCoroutine { cont ->
                    val request = Request.Builder().url(url).build()
                    val ws = client.newWebSocket(
                        request,
                        object : WebSocketListener() {
                            override fun onOpen(webSocket: WebSocket, response: Response) {
                                webSocket.close(1000, "test")
                                if (cont.isActive) cont.resume(Result.success(Unit))
                            }

                            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                                if (cont.isActive) cont.resume(Result.failure(t))
                            }
                        },
                    )
                    cont.invokeOnCancellation { ws.cancel() }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * HTTP GET（如 `{base}/health`），响应码 2xx 视为成功。
     */
    fun testHttpGet(client: OkHttpClient, url: String): Result<Unit> {
        return try {
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("HTTP ${resp.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
