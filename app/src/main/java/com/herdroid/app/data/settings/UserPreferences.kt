package com.herdroid.app.data.settings

import com.herdroid.app.domain.HealthCheckUrlFactory

data class UserPreferences(
    val scheme: String,
    val host: String,
    val port: Int,
    val apiKey: String,
    val modelName: String,
    /** 主界面开关：收到助手回复后是否自动用系统 TTS 朗读。 */
    val autoPlayTts: Boolean,
    /** Runs 事件流断线后的自动重连次数（0 表示仅手动继续）。 */
    val runsAutoReconnectAttempts: Int,
) {
    /** OpenAI 兼容 API 根地址（POST …/v1/chat/completions），由协议、地址、端口拼接，不含路径。 */
    val apiBaseUrl: String
        get() = HealthCheckUrlFactory.buildHttpOrigin(scheme, host, port).orEmpty()

    companion object {
        val DEFAULT = UserPreferences(
            scheme = "http",
            host = "192.168.3.112",
            port = 8642,
            apiKey = "myapiky",
            modelName = "hermes-agent",
            autoPlayTts = false,
            runsAutoReconnectAttempts = 3,
        )
    }
}
