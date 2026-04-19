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
        /** 默认与局域网 Hermes 测试环境一致（可在设置中修改）。 */
        val DEFAULT = UserPreferences(
            scheme = "http",
            host = "192.168.3.112",
            port = 8642,
            autoPlayTts = false,
            ttsEngine = TtsEngineType.SYSTEM,
            networkTtsBaseUrl = "http://192.168.3.112:8642",
            networkTtsApiKey = "myapiky",
            networkTtsModel = "hermes-agent",
        )
    }
}
