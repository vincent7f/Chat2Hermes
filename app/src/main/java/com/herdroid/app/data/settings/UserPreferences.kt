package com.herdroid.app.data.settings

enum class TtsEngineType {
    SYSTEM,
    NETWORK,
}

data class UserPreferences(
    val scheme: String,
    val host: String,
    val port: Int,
    val autoPlayTts: Boolean,
    val ttsEngine: TtsEngineType,
    val networkTtsBaseUrl: String,
    val networkTtsApiKey: String,
    /** Hermes / OpenAI 兼容接口中的模型名（如局域网 TTS 或后续 AI 调用）。 */
    val networkTtsModel: String,
) {
    companion object {
        /** 与 Hermes 默认 LAN 服务一致；需提供 API Key（DataStore 中可为空，由用户在设置中填写）。 */
        val DEFAULT = UserPreferences(
            scheme = "http",
            host = "192.168.3.112",
            port = 8642,
            autoPlayTts = false,
            ttsEngine = TtsEngineType.SYSTEM,
            networkTtsBaseUrl = "http://192.168.3.112:8642",
            networkTtsApiKey = "",
            networkTtsModel = "hermes-agent",
        )
    }
}
