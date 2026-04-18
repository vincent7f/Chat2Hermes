package com.herdroid.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.data.ha.HaClient
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.WebSocketUrlFactory
import com.herdroid.app.domain.tts.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectionParams(
    val scheme: String,
    val host: String,
    val port: Int,
)

class MainViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val haClient: HaClient,
    private val ttsManager: TtsManager,
) : AndroidViewModel(application) {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserPreferences.DEFAULT,
        )

    val connectionState = haClient.connectionState

    private val _lastMessage = MutableStateFlow("")
    val lastMessage: StateFlow<String> = _lastMessage.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.preferencesFlow
                .map { ConnectionParams(it.scheme, it.host, it.port) }
                .distinctUntilChanged()
                .collectLatest { params ->
                    haClient.disconnect()
                    val url = WebSocketUrlFactory.build(params.scheme, params.host, params.port)
                    if (url != null) {
                        haClient.connect(url)
                    }
                }
        }
        viewModelScope.launch {
            haClient.messages.collect { msg ->
                _lastMessage.value = msg
                val prefs = preferences.value
                if (prefs.autoPlayTts) {
                    ttsManager.speak(
                        rawText = msg,
                        engine = prefs.ttsEngine,
                        networkBaseUrl = prefs.networkTtsBaseUrl,
                        networkApiKey = prefs.networkTtsApiKey,
                        networkModel = prefs.networkTtsModel,
                        onNetworkError = { err -> _userMessage.value = err },
                    )
                }
            }
        }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayTts(enabled)
        }
    }

    fun reconnect() {
        viewModelScope.launch {
            val p = preferences.value
            haClient.disconnect()
            val url = WebSocketUrlFactory.build(p.scheme, p.host, p.port)
            if (url != null) {
                haClient.connect(url)
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Keep HA connection for app lifecycle; optional: haClient.disconnect()
    }
}
