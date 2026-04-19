package com.herdroid.app

import android.app.Application
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.domain.tts.TtsManager
import okhttp3.OkHttpClient

class HerdroidApplication : Application() {

    val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var ttsManager: TtsManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        ttsManager = TtsManager(this, okHttpClient)
    }
}
