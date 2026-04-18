package com.herdroid.app.domain

object WebSocketUrlFactory {

    fun build(scheme: String, host: String, port: Int): String? {
        val h = host.trim()
        if (h.isEmpty()) return null
        val wsScheme = when (scheme.lowercase().trim()) {
            "http", "ws" -> "ws"
            "https", "wss" -> "wss"
            else -> "ws"
        }
        return if (port > 0 && port != 80 && port != 443) {
            "$wsScheme://$h:$port"
        } else {
            "$wsScheme://$h"
        }
    }
}
