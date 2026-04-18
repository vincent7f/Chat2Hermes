package com.herdroid.app.domain

/**
 * Default WebSocket scan lists for Hermes / LAN agent discovery ([com.herdroid.app.ui.settings.SettingsViewModel]).
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

    val DEFAULT_WS_SCHEMES: List<String> = listOf("ws", "wss")
}
