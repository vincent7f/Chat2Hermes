package com.herdroid.app.data.chat

import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.HealthCheckUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 主界面与设置页与 Hermes 的 OpenAI 兼容对话共用：同一套根 URL（[HealthCheckUrlFactory.buildHttpOrigin]）与 [OpenAiChatClient.chatCompletions]。
 */
object OpenAiChatFromSettings {

    data class Prepared(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
    )

    sealed class PrepareOutcome {
        data class Ready(val prepared: Prepared) : PrepareOutcome()
        data object PortInvalid : PrepareOutcome()
        data object BaseUrlInvalid : PrepareOutcome()
        data object ApiKeyMissing : PrepareOutcome()
    }

    fun prepareFromPreferences(prefs: UserPreferences): PrepareOutcome =
        prepareWithIntPort(
            scheme = prefs.scheme,
            host = prefs.host,
            port = prefs.port,
            apiKey = prefs.apiKey,
            model = prefs.modelName,
        )

    /** 与 [prepareFromPreferences] 等价：先解析端口再拼 URL，供设置页表单与 [UserPreferences] 对齐。 */
    fun prepareFromPortText(
        scheme: String,
        host: String,
        portText: String,
        apiKey: String,
        model: String,
    ): PrepareOutcome {
        val port = portText.toIntOrNull()
        if (port == null || port !in 1..65535) return PrepareOutcome.PortInvalid
        return prepareWithIntPort(scheme, host, port, apiKey, model)
    }

    private fun prepareWithIntPort(
        scheme: String,
        host: String,
        port: Int,
        apiKey: String,
        model: String,
    ): PrepareOutcome {
        if (port !in 1..65535) return PrepareOutcome.PortInvalid
        val root = HealthCheckUrlFactory.buildHttpOrigin(scheme, host, port)
        if (root.isNullOrEmpty()) return PrepareOutcome.BaseUrlInvalid
        if (apiKey.isBlank()) return PrepareOutcome.ApiKeyMissing
        val m = model.trim().ifEmpty { "hermes-agent" }
        return PrepareOutcome.Ready(
            Prepared(baseUrl = root, apiKey = apiKey.trim(), model = m),
        )
    }

    fun complete(
        client: OpenAiChatClient,
        prepared: Prepared,
        messages: List<Pair<String, String>>,
    ): Result<String> = client.chatCompletions(
        baseUrl = prepared.baseUrl,
        apiKey = prepared.apiKey,
        model = prepared.model,
        messages = messages,
    )

    /** 与设置页「测试对话」、主界面发消息共用：在 IO 线程执行 [complete]。 */
    suspend fun executeCompletions(
        client: OpenAiChatClient,
        prepared: Prepared,
        messages: List<Pair<String, String>>,
    ): Result<String> = withContext(Dispatchers.IO) {
        complete(client, prepared, messages)
    }
}
