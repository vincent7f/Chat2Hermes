package com.herdroid.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.ha.HaClient
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.WebSocketUrlFactory
import com.herdroid.app.domain.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private val chatClient: OpenAiChatClient,
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

    private var chatMessageSeq = 0L

    private val _chatMessages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatUiMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

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

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _chatLoading.value) return
        val prefs = preferences.value
        val app = getApplication<Application>()
        if (prefs.networkTtsApiKey.isBlank()) {
            _userMessage.value = app.getString(R.string.chat_need_api_key)
            return
        }
        if (prefs.networkTtsBaseUrl.isBlank()) {
            _userMessage.value = app.getString(R.string.chat_need_base_url)
            return
        }
        viewModelScope.launch {
            _chatLoading.value = true
            try {
                val userMsg = ChatUiMessage(
                    id = ++chatMessageSeq,
                    role = ChatMessageRole.User,
                    text = trimmed,
                )
                _chatMessages.update { it + userMsg }

                val apiMessages = _chatMessages.value.map { m ->
                    when (m.role) {
                        ChatMessageRole.User -> "user" to m.text
                        ChatMessageRole.Assistant -> "assistant" to m.text
                    }
                }

                val result = withContext(Dispatchers.IO) {
                    chatClient.chatCompletions(
                        baseUrl = prefs.networkTtsBaseUrl,
                        apiKey = prefs.networkTtsApiKey,
                        model = prefs.networkTtsModel,
                        messages = apiMessages,
                    )
                }

                result.fold(
                    onSuccess = { reply ->
                        _chatMessages.update {
                            it + ChatUiMessage(
                                id = ++chatMessageSeq,
                                role = ChatMessageRole.Assistant,
                                text = reply,
                            )
                        }
                        val p = preferences.value
                        if (p.autoPlayTts) {
                            ttsManager.speak(
                                rawText = reply,
                                engine = p.ttsEngine,
                                networkBaseUrl = p.networkTtsBaseUrl,
                                networkApiKey = p.networkTtsApiKey,
                                networkModel = p.networkTtsModel,
                                onNetworkError = { err -> _userMessage.value = err },
                            )
                        }
                    },
                    onFailure = { t ->
                        _userMessage.value = app.getString(
                            R.string.chat_request_failed,
                            t.message ?: t.javaClass.simpleName,
                        )
                    },
                )
            } finally {
                _chatLoading.value = false
            }
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
    }
}
