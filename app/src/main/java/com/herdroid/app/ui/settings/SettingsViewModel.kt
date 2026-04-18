package com.herdroid.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herdroid.app.HerdroidApplication
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.TtsEngineType
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
) : ViewModel() {

    fun save(
        scheme: String,
        host: String,
        port: Int,
        autoPlayTts: Boolean,
        ttsEngine: TtsEngineType,
        networkTtsBaseUrl: String,
        networkTtsApiKey: String,
    ) {
        viewModelScope.launch {
            repository.update(
                scheme = scheme,
                host = host,
                port = port,
                autoPlayTts = autoPlayTts,
                ttsEngine = ttsEngine,
                networkTtsBaseUrl = networkTtsBaseUrl,
                networkTtsApiKey = networkTtsApiKey,
            )
        }
    }

    companion object {
        fun factory(app: HerdroidApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(app.settingsRepository) as T
                    }
                    throw IllegalArgumentException()
                }
            }
        }
    }
}
