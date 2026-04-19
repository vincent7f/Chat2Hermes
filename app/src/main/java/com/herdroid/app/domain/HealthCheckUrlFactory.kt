package com.herdroid.app.domain

/**
 * Hermes 等服务常用 HTTP 健康检查路径，例如 `http://192.168.3.112:8642/health`。
 * [scheme] 为设置中的访问协议：ws/http → http，wss/https → https。
 */
object HealthCheckUrlFactory {

    /**
     * OpenAI 兼容 API 根地址（不含路径），例如 `http://192.168.3.112:8642`。
     * 与 [build] 中主机、端口、协议规则一致。
     */
    fun buildHttpOrigin(scheme: String, host: String, port: Int): String? {
        val h = host.trim()
        if (h.isEmpty()) return null
        val httpScheme = when (scheme.lowercase().trim()) {
            "http", "ws" -> "http"
            "https", "wss" -> "https"
            else -> scheme.lowercase().trim().ifEmpty { "http" }
        }
        return if (port > 0 && port != 80 && port != 443) {
            "$httpScheme://$h:$port"
        } else {
            "$httpScheme://$h"
        }
    }

    fun build(scheme: String, host: String, port: Int): String? {
        val base = buildHttpOrigin(scheme, host, port) ?: return null
        return "$base/health"
    }
}
