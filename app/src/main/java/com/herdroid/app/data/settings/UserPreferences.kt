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
) {
    companion object {
        val DEFAULT = UserPreferences(
            scheme = "ws",
            host = "",
            port = 8080,
            autoPlayTts = false,
            ttsEngine = TtsEngineType.SYSTEM,
            networkTtsBaseUrl = "",
            networkTtsApiKey = "",
        )
    }
}
