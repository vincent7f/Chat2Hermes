package com.herdroid.app.data.settings

data class UserPreferences(
    val scheme: String,
    val host: String,
    val port: Int,
    /** OpenAI 兼容 API 根地址（POST …/v1/chat/completions），不含路径。 */
    val apiBaseUrl: String,
    val apiKey: String,
    val modelName: String,
) {
    companion object {
        val DEFAULT = UserPreferences(
            scheme = "http",
            host = "192.168.3.112",
            port = 8642,
            apiBaseUrl = "http://192.168.3.112:8642",
            apiKey = "myapiky",
            modelName = "hermes-agent",
        )
    }
}
