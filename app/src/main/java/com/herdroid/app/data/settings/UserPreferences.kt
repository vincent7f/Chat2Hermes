package com.herdroid.app.data.settings

import com.herdroid.app.domain.HealthCheckUrlFactory

data class UserPreferences(
    val scheme: String,
    val host: String,
    val port: Int,
    val apiKey: String,
    val modelName: String,
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
        )
    }
}
