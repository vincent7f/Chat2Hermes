package com.herdroid.app

import android.app.Application
import com.herdroid.app.data.chat.ChatSessionRepository
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.chat.HermesRunsClient
import com.herdroid.app.data.settings.SettingsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HerdroidApplication : Application() {

    /** 健康检查、短 GET 等；保持默认较短超时，避免挂死连接拖很久。 */
    val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    /**
     * 仅用于 `POST …/v1/chat/completions`。
     * Hermes Agent 可能在单次请求内执行工具调用，响应时间远长于普通 REST；
     * OkHttp 默认 [read timeout][OkHttpClient.Builder.readTimeout] 过短会导致误报超时。
     *
     * 设计说明见 [docs/HERMES_API_SERVER.md](docs/HERMES_API_SERVER.md)。
     */
    private val chatCompletionsHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
            .readTimeout(CHAT_READ_TIMEOUT_MIN, TimeUnit.MINUTES)
            .writeTimeout(CHAT_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.SECONDS)
            .build()
    }

    /** 主界面与设置页 Hermes 对话共用；使用为本客户端单独配置的较长读超时。 */
    val openAiChatClient: OpenAiChatClient by lazy { OpenAiChatClient(chatCompletionsHttpClient) }
    /** 主界面长会话优先使用 Runs API。 */
    val hermesRunsClient: HermesRunsClient by lazy { HermesRunsClient(chatCompletionsHttpClient) }

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var chatSessionRepository: ChatSessionRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        runBlocking { settingsRepository.migrateFromLegacyIfNeeded() }
        chatSessionRepository = ChatSessionRepository(this)
    }

    private companion object {
        private const val CONNECT_TIMEOUT_SEC = 30L
        private const val CHAT_WRITE_TIMEOUT_SEC = 120L
        /** 非流式整段回复：需覆盖工具调用等长耗时。 */
        private const val CHAT_READ_TIMEOUT_MIN = 15L
    }
}
