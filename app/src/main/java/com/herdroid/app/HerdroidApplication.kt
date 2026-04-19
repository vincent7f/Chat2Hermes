package com.herdroid.app

import android.app.Application
import com.herdroid.app.data.settings.SettingsRepository
import okhttp3.OkHttpClient

class HerdroidApplication : Application() {

    val okHttpClient: OkHttpClient by lazy { OkHttpClient() }

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
