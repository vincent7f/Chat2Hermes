package com.herdroid.app.ui.main

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.IOException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.herdroid.app.R
import com.herdroid.app.data.chat.ChatSessionRepository
import com.herdroid.app.data.chat.HermesRunsClient
import com.herdroid.app.data.chat.OpenAiChatFromSettings
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.MediaVolumeChecker
import com.herdroid.app.domain.hasActiveNetwork
import com.herdroid.app.domain.tts.LyricLineWindow
import com.herdroid.app.domain.tts.LyricPlaybackCallback
import com.herdroid.app.domain.tts.SystemTtsSpeaker
import com.herdroid.app.ui.hermes.errorMessage
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import java.util.ArrayDeque
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** 主界面 Hermes 对话；可选在收到助手回复后用系统 TTS 朗读。 */
class MainViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val runsClient: HermesRunsClient,
) : AndroidViewModel(application) {

    private val ttsSpeaker = SystemTtsSpeaker(application)

    private val mainHandler = Handler(Looper.getMainLooper())

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserPreferences.DEFAULT,
        )
    val activeProfileId: StateFlow<String> = settingsRepository.activeProfileId
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "default",
        )
    val profileIds: StateFlow<List<String>> = settingsRepository.profileIds
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            listOf("default"),
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

    /** 朗读请求队列：正在播放一段时，新回复进入队列，按顺序播完。 */
    private val ttsSpeakQueue = ArrayDeque<String>()
    private var ttsQueuePlaybackActive = false

    /** 因输入法打开而暂停朗读；关闭 IME 或发送消息后恢复。 */
    private var ttsPausedForIme = false

    private var chatMessageSeq = 0L

    /** 客户端生成的对话会话 id，随 [persistCurrentConversation] 写入本地。 */
    private var conversationSessionId: String? = null

    private val _chatMessages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatUiMessage>> = _chatMessages.asStateFlow()
    private var pendingRunResume: PendingRunResume? = null
    private val _resumableAssistantMessageId = MutableStateFlow<Long?>(null)
    val resumableAssistantMessageId: StateFlow<Long?> = _resumableAssistantMessageId.asStateFlow()
    private val _resumeRunPrompt = MutableStateFlow<ResumeRunPrompt?>(null)
    val resumeRunPrompt: StateFlow<ResumeRunPrompt?> = _resumeRunPrompt.asStateFlow()

    /**
     * 若存在上次持久化的消息，则非 `null`，主界面展示「继续 / 新对话」。
     */
    private val _resumeConversationPrompt = MutableStateFlow<ResumeConversationPrompt?>(null)
    val resumeConversationPrompt: StateFlow<ResumeConversationPrompt?> =
        _resumeConversationPrompt.asStateFlow()

    init {
        viewModelScope.launch {
            val json = chatSessionRepository.messagesJsonFlow.first()
            if (!json.isNullOrBlank()) {
                val sid = chatSessionRepository.sessionIdFlow.first()
                _resumeConversationPrompt.value = ResumeConversationPrompt(
                    messagesJson = json,
                    sessionId = sid,
                )
            }
        }
    }

    /**
     * 每次成功发起一轮新发送（用户消息 + 占位助手消息入列）时递增；
     * UI 用于收起列表中所有已展开的气泡。
     */
    private val _collapseExpandEpoch = MutableStateFlow(0L)
    val collapseExpandEpoch: StateFlow<Long> = _collapseExpandEpoch.asStateFlow()

    fun setAutoPlayTts(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoPlayTts(enabled)
        }
    }

    fun switchProfile(profileId: String) {
        viewModelScope.launch {
            runCatching { settingsRepository.setActiveProfile(profileId) }
                .onSuccess { clearChat() }
        }
    }

    fun clearChat() {
        dismissTtsLyric()
        conversationSessionId = null
        chatMessageSeq = 0L
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            chatSessionRepository.clearPersistedConversation()
        }
    }

    /** 启动时选择继续上次已持久化的会话。 */
    fun continuePersistedConversation() {
        val p = _resumeConversationPrompt.value ?: return
        _resumeConversationPrompt.value = null
        _chatMessages.value = parseChatMessagesJson(p.messagesJson)
        conversationSessionId = p.sessionId
    }

    /** 启动时放弃持久化记录，开始空会话。 */
    fun discardPersistedAndStartNew() {
        _resumeConversationPrompt.value = null
        conversationSessionId = null
        chatMessageSeq = 0L
        _chatMessages.value = emptyList()
        viewModelScope.launch {
            chatSessionRepository.clearPersistedConversation()
        }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return
        resumeTtsIfPausedForImeAfterSend()
        val app = getApplication<Application>()
        viewModelScope.launch {
            val prepared = prepareChatRequest(app) ?: return@launch
            val pending = enqueuePendingMessages(trimmed)
            val prefs = settingsRepository.preferencesFlow.first()
            val createRun = withContext(Dispatchers.IO) {
                runsClient.createRun(
                    baseUrl = prepared.baseUrl,
                    apiKey = prepared.apiKey,
                    model = prepared.model,
                    messages = pending.apiMessages,
                )
            }
            val runId = createRun.getOrElse { t ->
                onChatFailure(app, pending.userMsgId, pending.assistantMsgId, t)
                return@launch
            }

            streamExistingRun(
                app = app,
                prepared = prepared,
                runId = runId,
                userMsgId = pending.userMsgId,
                assistantMsgId = pending.assistantMsgId,
                maxReconnectAttempts = prefs.runsAutoReconnectAttempts,
                allowManualResumePrompt = true,
            )
        }
    }

    fun continueFailedRunSubscription() {
        val pending = pendingRunResume ?: return
        _resumeRunPrompt.value = null
        val app = getApplication<Application>()
        viewModelScope.launch {
            markAsResumingAfterManualContinue(pending.userMsgId, pending.assistantMsgId)
            streamExistingRun(
                app = app,
                prepared = pending.prepared,
                runId = pending.runId,
                userMsgId = pending.userMsgId,
                assistantMsgId = pending.assistantMsgId,
                maxReconnectAttempts = pending.maxReconnectAttempts,
                allowManualResumePrompt = true,
            )
        }
    }

    fun keepRunForLaterResume() {
        _resumeRunPrompt.value = null
    }

    fun dismissRunResumePrompt(markAsFailed: Boolean = true) {
        val pending = pendingRunResume
        _resumeRunPrompt.value = null
        setPendingRunResume(null)
        if (!markAsFailed || pending == null) return
        markAsFailedAfterResumeAborted(pending.userMsgId, pending.assistantMsgId)
    }

    fun stopWaitingCurrentRun() {
        val pending = pendingRunResume ?: run {
            _resumeRunPrompt.value = null
            return
        }
        _resumeRunPrompt.value = null
        setPendingRunResume(null)
        markAsFailedAfterResumeAborted(pending.userMsgId, pending.assistantMsgId)
    }

    private suspend fun prepareChatRequest(app: Application): OpenAiChatFromSettings.Prepared? {
        val prefs = settingsRepository.preferencesFlow.first()
        val outcome = OpenAiChatFromSettings.prepareFromPreferences(prefs)
        outcome.errorMessage(app)?.let {
            _userMessage.value = it
            return null
        }
        if (!app.hasActiveNetwork()) {
            _userMessage.value = app.getString(R.string.test_feedback_network_unavailable)
            return null
        }
        return (outcome as OpenAiChatFromSettings.PrepareOutcome.Ready).prepared
    }

    private fun enqueuePendingMessages(trimmed: String): PendingChatRequest {
        val nowMillis = System.currentTimeMillis()
        val userMsg = ChatUiMessage(
            id = ++chatMessageSeq,
            role = ChatMessageRole.User,
            text = trimmed,
            userSendState = UserMessageSendState.Sending,
            timeMillis = nowMillis,
        )
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
        _collapseExpandEpoch.update { it + 1 }
        _chatMessages.update { it + userMsg + assistantMsg }
        return PendingChatRequest(
            userMsgId = userMsg.id,
            assistantMsgId = assistantMsg.id,
            apiMessages = apiMessages,
        )
    }

    private fun applyAssistantDelta(assistantMsgId: Long, delta: String) {
        updateMessageById(assistantMsgId) { m ->
            m.copy(text = m.text + delta, replyComplete = false)
        }
    }

    private suspend fun onChatSuccess(userMsgId: Long, assistantMsgId: Long, fullReply: String) {
        updateMessageById(userMsgId) { m -> m.copy(userSendState = UserMessageSendState.Sent) }
        updateMessageById(assistantMsgId) { m ->
            m.copy(text = fullReply, replyComplete = true)
        }
        val latestPrefs = settingsRepository.preferencesFlow.first()
        if (latestPrefs.autoPlayTts) {
            speakWithOptionalVolumeWarning(fullReply)
        }
        persistCurrentConversation()
    }

    private fun onChatFailure(app: Application, userMsgId: Long, assistantMsgId: Long, t: Throwable) {
        _chatMessages.update { list ->
            val markedUser = list.map { m ->
                if (m.id == userMsgId) m.copy(userSendState = UserMessageSendState.Failed) else m
            }
            val asst = markedUser.find { it.id == assistantMsgId }
            if (asst == null || asst.text.isBlank()) {
                markedUser.filterNot { it.id == assistantMsgId }
            } else {
                markedUser.map { m ->
                    if (m.id == assistantMsgId) m.copy(replyComplete = true) else m
                }
            }
        }
        _userMessage.value = app.getString(
            R.string.chat_request_failed,
            t.message ?: t.javaClass.simpleName,
        )
    }

    private suspend fun streamExistingRun(
        app: Application,
        prepared: OpenAiChatFromSettings.Prepared,
        runId: String,
        userMsgId: Long,
        assistantMsgId: Long,
        maxReconnectAttempts: Int,
        allowManualResumePrompt: Boolean,
    ) {
        val result = withContext(Dispatchers.IO) {
            runsClient.continueRunAndCollectText(
                baseUrl = prepared.baseUrl,
                apiKey = prepared.apiKey,
                runId = runId,
                onContentDelta = { delta ->
                    mainHandler.post { applyAssistantDelta(assistantMsgId, delta) }
                },
                maxReconnectAttempts = maxReconnectAttempts,
                onReconnect = {
                    mainHandler.post {
                        _userMessage.value = app.getString(R.string.chat_reconnected_receiving)
                    }
                },
            )
        }
        result.fold(
            onSuccess = { fullReply ->
                setPendingRunResume(null)
                _resumeRunPrompt.value = null
                onChatSuccess(userMsgId, assistantMsgId, fullReply)
            },
            onFailure = { t ->
                if (allowManualResumePrompt && canPromptResume(t)) {
                    setPendingRunResume(
                        PendingRunResume(
                            prepared = prepared,
                            runId = runId,
                            userMsgId = userMsgId,
                            assistantMsgId = assistantMsgId,
                            maxReconnectAttempts = maxReconnectAttempts,
                        ),
                    )
                    markAsFailedAfterResumeAborted(userMsgId, assistantMsgId)
                    _resumeRunPrompt.value = ResumeRunPrompt(runId = runId)
                    _userMessage.value = app.getString(R.string.chat_request_failed, t.message ?: t.javaClass.simpleName)
                } else {
                    setPendingRunResume(null)
                    _resumeRunPrompt.value = null
                    onChatFailure(app, userMsgId, assistantMsgId, t)
                }
            },
        )
    }

    private fun setPendingRunResume(value: PendingRunResume?) {
        pendingRunResume = value
        _resumableAssistantMessageId.value = value?.assistantMsgId
    }

    private fun markAsResumingAfterManualContinue(userMsgId: Long, assistantMsgId: Long) {
        _chatMessages.update { list ->
            list.map { m ->
                when (m.id) {
                    userMsgId -> m.copy(userSendState = UserMessageSendState.Sending)
                    assistantMsgId -> m.copy(replyComplete = false)
                    else -> m
                }
            }
        }
    }

    private fun canPromptResume(t: Throwable): Boolean {
        if (t is IOException) return true
        val msg = t.message.orEmpty().lowercase()
        return msg.contains("failed to connect") ||
            msg.contains("timeout") ||
            msg.contains("connection reset") ||
            msg.contains("stream was reset")
    }

    private fun markAsFailedAfterResumeAborted(userMsgId: Long, assistantMsgId: Long) {
        _chatMessages.update { list ->
            val markedUser = list.map { m ->
                if (m.id == userMsgId) m.copy(userSendState = UserMessageSendState.Failed) else m
            }
            val asst = markedUser.find { it.id == assistantMsgId }
            if (asst == null || asst.text.isBlank()) {
                markedUser.filterNot { it.id == assistantMsgId }
            } else {
                markedUser.map { m ->
                    if (m.id == assistantMsgId) m.copy(replyComplete = true) else m
                }
            }
        }
    }

    private fun updateMessageById(messageId: Long, transform: (ChatUiMessage) -> ChatUiMessage) {
        _chatMessages.update { list ->
            list.map { m ->
                if (m.id == messageId) transform(m) else m
            }
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
        ttsSpeakQueue.addLast(plainText)
        if (!ttsQueuePlaybackActive) {
            playNextTtsFromQueue()
        }
    }

    private fun playNextTtsFromQueue() {
        if (ttsSpeakQueue.isEmpty()) {
            ttsQueuePlaybackActive = false
            _ttsLyric.value = null
            return
        }
        ttsQueuePlaybackActive = true
        val plainText = ttsSpeakQueue.removeFirst()
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
                    mainHandler.post {
                        _ttsLyric.value = null
                        playNextTtsFromQueue()
                    }
                }

                override fun onError(message: String) {
                    mainHandler.post {
                        _ttsLyric.value = null
                        playNextTtsFromQueue()
                    }
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

    /**
     * 主界面观察 [WindowInsets.ime]：IME 显示且正在/排队朗读时暂停；IME 隐藏后恢复。
     */
    fun onImeVisibilityChanged(imeVisible: Boolean) {
        if (!imeVisible) {
            if (ttsPausedForIme) {
                ttsPausedForIme = false
                resumeTtsLyric()
            }
            return
        }
        val playingOrQueued =
            ttsQueuePlaybackActive || ttsSpeakQueue.isNotEmpty() || _ttsLyric.value != null
        if (!playingOrQueued) return
        val alreadyPaused = _ttsLyric.value?.isPaused == true
        if (!alreadyPaused) {
            pauseTtsLyric()
            ttsPausedForIme = true
        }
    }

    private fun resumeTtsIfPausedForImeAfterSend() {
        if (ttsPausedForIme) {
            ttsPausedForIme = false
            resumeTtsLyric()
        }
    }

    fun dismissTtsLyric() {
        ttsPausedForIme = false
        ttsSpeakQueue.clear()
        ttsQueuePlaybackActive = false
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

    private fun persistCurrentConversation() {
        viewModelScope.launch {
            val list = _chatMessages.value
            if (list.isEmpty()) return@launch
            val sid = conversationSessionId ?: UUID.randomUUID().toString().also {
                conversationSessionId = it
            }
            chatSessionRepository.persistConversation(sid, chatMessagesToJson(list))
        }
    }

    private fun chatMessagesToJson(list: List<ChatUiMessage>): String {
        val arr = JSONArray()
        for (m in list) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("role", if (m.role == ChatMessageRole.User) "user" else "assistant")
            o.put("text", m.text)
            o.put("replyComplete", m.replyComplete)
            o.put("timeMillis", m.timeMillis)
            when (m.userSendState) {
                UserMessageSendState.Sending -> o.put("userSendState", "sending")
                UserMessageSendState.Sent -> o.put("userSendState", "sent")
                UserMessageSendState.Failed -> o.put("userSendState", "failed")
                null -> o.put("userSendState", JSONObject.NULL)
            }
            arr.put(o)
        }
        return arr.toString()
    }

    private fun parseChatMessagesJson(json: String): List<ChatUiMessage> {
        val arr = JSONArray(json)
        val out = ArrayList<ChatUiMessage>(arr.length())
        var maxId = 0L
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.optLong("id", (i + 1).toLong())
            maxId = maxOf(maxId, id)
            val role = when (o.optString("role", "user")) {
                "assistant" -> ChatMessageRole.Assistant
                else -> ChatMessageRole.User
            }
            val us = when (o.optString("userSendState", "")) {
                "sending" -> UserMessageSendState.Sending
                "sent" -> UserMessageSendState.Sent
                "failed" -> UserMessageSendState.Failed
                else -> null
            }
            out.add(
                ChatUiMessage(
                    id = id,
                    role = role,
                    text = o.optString("text", ""),
                    userSendState = us,
                    replyComplete = o.optBoolean("replyComplete", true),
                    timeMillis = o.optLong("timeMillis", System.currentTimeMillis()),
                ),
            )
        }
        chatMessageSeq = maxId
        return out
    }

    companion object {
        private const val VOLUME_WARNING_MS = 5_000L
    }
}

data class ResumeConversationPrompt(
    val messagesJson: String,
    val sessionId: String?,
)

data class TtsLyricUiState(
    val previousLine: String,
    val currentLine: String,
    val nextLine: String,
    val lineIndex: Int,
    val lineCount: Int,
    val isPaused: Boolean,
)

data class ResumeRunPrompt(
    val runId: String,
)

private data class PendingChatRequest(
    val userMsgId: Long,
    val assistantMsgId: Long,
    val apiMessages: List<Pair<String, String>>,
)

private data class PendingRunResume(
    val prepared: OpenAiChatFromSettings.Prepared,
    val runId: String,
    val userMsgId: Long,
    val assistantMsgId: Long,
    val maxReconnectAttempts: Int,
)
