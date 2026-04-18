package com.herdroid.app.domain

import okhttp3.OkHttpClient

/**
 * Tries [wsSchemes] × [ports] on [host] until a WebSocket handshake succeeds (same probe as [HaConnectionTester]).
 */
object HaEndpointScanner {

    /** 至少包含用户要求的 80、8000、8080、8765，并补充常见端口。 */
    val DEFAULT_PORTS: List<Int> = listOf(
        80,
        8000,
        8080,
        8765,
        443,
        8642,
    )

    val DEFAULT_WS_SCHEMES: List<String> = listOf("ws", "wss")

    suspend fun findFirstWebSocket(
        client: OkHttpClient,
        host: String,
        schemes: List<String> = DEFAULT_WS_SCHEMES,
        ports: List<Int> = DEFAULT_PORTS,
        timeoutMsPerAttempt: Long = 3_500L,
    ): Result<Pair<String, Int>> {
        val h = host.trim()
        if (h.isEmpty()) return Result.failure(IllegalArgumentException("empty host"))
        for (sch in schemes) {
            for (port in ports) {
                val url = WebSocketUrlFactory.build(sch, h, port) ?: continue
                val r = HaConnectionTester.testWebSocket(client, url, timeoutMsPerAttempt)
                if (r.isSuccess) {
                    return Result.success(sch to port)
                }
            }
        }
        return Result.failure(NoSuchElementException("no endpoint"))
    }
}
