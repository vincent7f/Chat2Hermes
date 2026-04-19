package com.herdroid.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.tts.TtsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主界面仅负责 **OpenAI 兼容** `POST /v1/chat/completions` 对话；API 基址、Bearer Key、模型名来自设置。
 */
class MainViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager,
    private val chatClient: OpenAiChatClient,
) : AndroidViewModel(application) {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserPreferences.DEFAULT,
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private var chatMessageSeq = 0L

    private val _chatMessages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatUiMessage>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

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
}
