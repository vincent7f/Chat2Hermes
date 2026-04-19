package com.herdroid.app.ui.main

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.chat.OpenAiChatFromSettings
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.MediaVolumeChecker
import com.herdroid.app.domain.hasActiveNetwork
import com.herdroid.app.domain.tts.LyricLineWindow
import com.herdroid.app.domain.tts.LyricPlaybackCallback
import com.herdroid.app.domain.tts.SystemTtsSpeaker
import com.herdroid.app.ui.hermes.errorMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val mainHandler = Handler(Looper.getMainLooper())

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserPreferences.DEFAULT,
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** 朗读前音量过低时在主界面展示的浮窗文案；约 5 秒后由 [dismissVolumeWarningAfterDelay] 清除。 */
    private val _volumeWarning = MutableStateFlow<String?>(null)
    val volumeWarning: StateFlow<String?> = _volumeWarning.asStateFlow()

    /** 歌词式朗读弹窗；`null` 表示未显示。 */
    private val _ttsLyric = MutableStateFlow<TtsLyricUiState?>(null)
    val ttsLyric: StateFlow<TtsLyricUiState?> = _ttsLyric.asStateFlow()

    private var volumeWarningDismissJob: Job? = null

    private var chatMessageSeq = 0L

    private val _chatMessages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatUiMessage>> = _chatMessages.asStateFlow()

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
        if (trimmed.isEmpty()) return
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

            val nowMillis = System.currentTimeMillis()
            val userMsg = ChatUiMessage(
                id = ++chatMessageSeq,
                role = ChatMessageRole.User,
                text = trimmed,
                userSendState = UserMessageSendState.Sending,
                timeMillis = nowMillis,
            )
            val userMsgId = userMsg.id
            val apiMessages = _chatMessages.value.map { m ->
                when (m.role) {
                    ChatMessageRole.User -> "user" to m.text
                    ChatMessageRole.Assistant -> "assistant" to m.text
                }
            } + ("user" to trimmed)

            val assistantMsg = ChatUiMessage(
                id = ++chatMessageSeq,
                role = ChatMessageRole.Assistant,
                text = "",
                replyComplete = false,
                timeMillis = nowMillis,
            )
            val assistantMsgId = assistantMsg.id
            _chatMessages.update { it + userMsg + assistantMsg }

            val result = OpenAiChatFromSettings.executeCompletionsStreaming(
                chatClient,
                prepared,
                apiMessages,
            ) { delta ->
                mainHandler.post {
                    _chatMessages.update { list ->
                        list.map { m ->
                            if (m.id == assistantMsgId) {
                                m.copy(text = m.text + delta, replyComplete = false)
                            } else {
                                m
                            }
                        }
                    }
                }
            }

            result.fold(
                onSuccess = { fullReply ->
                    _chatMessages.update { list ->
                        list.map { m ->
                            when (m.id) {
                                userMsgId -> m.copy(userSendState = UserMessageSendState.Sent)
                                assistantMsgId -> m.copy(text = fullReply, replyComplete = true)
                                else -> m
                            }
                        }
                    }
                    val latestPrefs = settingsRepository.preferencesFlow.first()
                    if (latestPrefs.autoPlayTts) {
                        speakWithOptionalVolumeWarning(fullReply)
                    }
                },
                onFailure = { t ->
                    _chatMessages.update { list ->
                        val markedUser = list.map { m ->
                            if (m.id == userMsgId) {
                                m.copy(userSendState = UserMessageSendState.Failed)
                            } else {
                                m
                            }
                        }
                        val asst = markedUser.find { it.id == assistantMsgId }
                        if (asst == null || asst.text.isBlank()) {
                            markedUser.filterNot { it.id == assistantMsgId }
                        } else {
                            markedUser.map { m ->
                                if (m.id == assistantMsgId) {
                                    m.copy(replyComplete = true)
                                } else {
                                    m
                                }
                            }
                        }
                    }
                    _userMessage.value = app.getString(
                        R.string.chat_request_failed,
                        t.message ?: t.javaClass.simpleName,
                    )
                },
            )
        }
    }

    /** 长按菜单「朗读」：与自动朗读相同，含音量过低浮窗提示。 */
    fun readMessageAloud(text: String) {
        speakWithOptionalVolumeWarning(text)
    }

    fun copyMessageToClipboard(text: String) {
        val app = getApplication<Application>()
        val cm = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val label = app.getString(R.string.clipboard_message_label)
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun speakWithOptionalVolumeWarning(plainText: String) {
        val app = getApplication<Application>()
        if (MediaVolumeChecker.isMediaVolumeBelowHalf(app)) {
            showVolumeWarning(app.getString(R.string.volume_warning_low_tts))
        } else {
            volumeWarningDismissJob?.cancel()
            _volumeWarning.value = null
        }
        ttsSpeaker.speakWithLyrics(
            plainText,
            object : LyricPlaybackCallback {
                override fun onLineWindow(window: LyricLineWindow) {
                    _ttsLyric.value = TtsLyricUiState(
                        previousLine = window.previous,
                        currentLine = window.current,
                        nextLine = window.next,
                        lineIndex = window.index,
                        lineCount = window.total,
                        isPaused = false,
                    )
                }

                override fun onComplete() {
                    _ttsLyric.value = null
                }

                override fun onError(message: String) {
                    _ttsLyric.value = null
                }
            },
        )
    }

    fun pauseTtsLyric() {
        ttsSpeaker.pauseLyricPlayback()
        _ttsLyric.update { it?.copy(isPaused = true) }
    }

    fun resumeTtsLyric() {
        ttsSpeaker.resumeLyricPlayback()
        _ttsLyric.update { it?.copy(isPaused = false) }
    }

    fun dismissTtsLyric() {
        ttsSpeaker.stopLyricPlayback()
        _ttsLyric.value = null
    }

    private fun showVolumeWarning(message: String) {
        _volumeWarning.value = message
        volumeWarningDismissJob?.cancel()
        volumeWarningDismissJob = viewModelScope.launch {
            delay(VOLUME_WARNING_MS)
            _volumeWarning.value = null
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        volumeWarningDismissJob?.cancel()
        ttsSpeaker.shutdown()
        super.onCleared()
    }

    companion object {
        private const val VOLUME_WARNING_MS = 5_000L
    }
}

data class TtsLyricUiState(
    val previousLine: String,
    val currentLine: String,
    val nextLine: String,
    val lineIndex: Int,
    val lineCount: Int,
    val isPaused: Boolean,
)
