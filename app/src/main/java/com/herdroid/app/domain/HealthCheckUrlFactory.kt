package com.herdroid.app.domain

/**
 * Hermes 等服务常用 HTTP 健康检查路径，例如 `http://192.168.3.112:8642/health`。
 * [scheme] 为设置中的访问协议：ws/http → http，wss/https → https。
 */
object HealthCheckUrlFactory {

    fun build(scheme: String, host: String, port: Int): String? {
        val h = host.trim()
        if (h.isEmpty()) return null
        val httpScheme = when (scheme.lowercase().trim()) {
            "http", "ws" -> "http"
            "https", "wss" -> "https"
            else -> scheme.lowercase().trim().ifEmpty { "http" }
        }
        val base = if (port > 0 && port != 80 && port != 443) {
            "$httpScheme://$h:$port"
        } else {
            "$httpScheme://$h"
        }
        return "$base/health"
    }
}
