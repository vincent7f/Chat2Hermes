package com.herdroid.app

import android.app.Application
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.settings.SettingsRepository
import okhttp3.OkHttpClient

class HerdroidApplication : Application() {

    val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    /** 主界面与设置页 Hermes 对话共用同一 [OpenAiChatClient]。 */
    val openAiChatClient: OpenAiChatClient by lazy { OpenAiChatClient(okHttpClient) }

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
