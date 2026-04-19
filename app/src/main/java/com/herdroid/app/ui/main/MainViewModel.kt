package com.herdroid.app.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.chat.OpenAiChatFromSettings
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.MediaVolumeChecker
import com.herdroid.app.domain.hasActiveNetwork
import com.herdroid.app.domain.tts.SystemTtsSpeaker
import com.herdroid.app.ui.hermes.errorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 主界面 Hermes 对话；可选在收到助手回复后用系统 TTS 朗读。 */
class MainViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val chatClient: OpenAiChatClient,
) : AndroidViewModel(application) {

    private val ttsSpeaker = SystemTtsSpeaker(application)

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

    fun setAutoPlayTts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayTts(enabled)
        }
    }

    fun clearChat() {
        _chatMessages.value = emptyList()
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _chatLoading.value) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            val prefs = settingsRepository.preferencesFlow.first()
            val outcome = OpenAiChatFromSettings.prepareFromPreferences(prefs)
            outcome.errorMessage(app)?.let {
                _userMessage.value = it
                return@launch
            }
            val prepared = (outcome as OpenAiChatFromSettings.PrepareOutcome.Ready).prepared
            if (!app.hasActiveNetwork()) {
                _userMessage.value = app.getString(R.string.test_feedback_network_unavailable)
                return@launch
            }
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

                val result = OpenAiChatFromSettings.executeCompletions(
                    chatClient,
                    prepared,
                    apiMessages,
                )

                result.fold(
                    onSuccess = { reply ->
                        _chatMessages.update {
                            it + ChatUiMessage(
                                id = ++chatMessageSeq,
                                role = ChatMessageRole.Assistant,
                                text = reply,
                            )
                        }
                        val latestPrefs = settingsRepository.preferencesFlow.first()
                        if (latestPrefs.autoPlayTts) {
                            if (MediaVolumeChecker.isMediaVolumeTooLow(app)) {
                                _userMessage.value = app.getString(R.string.volume_too_low_for_tts)
                            }
                            ttsSpeaker.speak(reply)
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
        ttsSpeaker.shutdown()
    }
}
