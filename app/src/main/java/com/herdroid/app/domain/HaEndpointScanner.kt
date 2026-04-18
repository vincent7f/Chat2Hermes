package com.herdroid.app.domain

/**
 * Default WebSocket scan lists for Hermes / LAN agent discovery ([com.herdroid.app.ui.settings.SettingsViewModel]).
 *
 * User-facing schemes include **http / https** (mapped to ws / wss URLs by [WebSocketUrlFactory]); [scanSteps]
 * deduplicates identical probe URLs so each endpoint is tried once while still preferring http/https labels.
 */
object HaEndpointScanner {

    const val DEFAULT_TIMEOUT_MS: Long = 3_500L

    /** 至少包含用户要求的 80、8000、8080、8765，并补充常见端口。 */
    val DEFAULT_PORTS: List<Int> = listOf(
        80,
        8000,
        8080,
        8765,
        443,
        8642,
    )

    /** 与设置页「访问协议」一致（仅 http / https）。 */
    private val SCAN_SCHEME_ORDER: List<String> = listOf("http", "https")

    data class ScanStep(
        /** 写入设置项时使用的协议（http 或 https）。 */
        val displayScheme: String,
        val port: Int,
        /** 实际 WebSocket 探测 URL（已由 [WebSocketUrlFactory] 规范化）。 */
        val probeUrl: String,
    )

    /**
     * 对每个端口依次尝试 [SCAN_SCHEME_ORDER]；若多个协议对应同一探测 URL，只保留最先出现的协议名，避免重复连接。
     */
    fun scanSteps(host: String): List<ScanStep> {
        val h = host.trim()
        if (h.isEmpty()) return emptyList()
        val seenUrls = mutableSetOf<String>()
        val out = ArrayList<ScanStep>()
        for (port in DEFAULT_PORTS) {
            for (sch in SCAN_SCHEME_ORDER) {
                val url = WebSocketUrlFactory.build(sch, h, port) ?: continue
                if (!seenUrls.add(url)) continue
                out.add(ScanStep(displayScheme = sch, port = port, probeUrl = url))
            }
        }
        return out
    }
}
