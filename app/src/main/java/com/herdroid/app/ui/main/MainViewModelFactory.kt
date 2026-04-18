package com.herdroid.app.ui.main

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.herdroid.app.HerdroidApplication
import com.herdroid.app.data.chat.OpenAiChatClient

class MainViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val app = application as HerdroidApplication
            return MainViewModel(
                application,
                app.settingsRepository,
                app.haClient,
                app.ttsManager,
                OpenAiChatClient(app.okHttpClient),
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
