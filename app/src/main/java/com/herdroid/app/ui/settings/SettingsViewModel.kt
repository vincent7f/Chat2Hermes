package com.herdroid.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herdroid.app.HerdroidApplication
import com.herdroid.app.R
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.TtsEngineType
import com.herdroid.app.domain.HaConnectionTester
import com.herdroid.app.domain.HaEndpointScanner
import com.herdroid.app.domain.WebSocketUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AutoDetectFill(val scheme: String, val port: Int)

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val app: HerdroidApplication,
) : AndroidViewModel(application) {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _autoDetectRunning = MutableStateFlow(false)
    val autoDetectRunning: StateFlow<Boolean> = _autoDetectRunning.asStateFlow()

    private val _pendingAutoFill = MutableStateFlow<AutoDetectFill?>(null)
    val pendingAutoFill: StateFlow<AutoDetectFill?> = _pendingAutoFill.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun consumePendingAutoFill() {
        _pendingAutoFill.value = null
    }

    /**
     * 根据已填主机（IP/域名）扫描 ws/wss 与 [HaEndpointScanner.DEFAULT_PORTS]，成功则写入 [pendingAutoFill] 供界面合并。
     */
    fun autoDetect(hostInput: String) {
        viewModelScope.launch {
            val host = hostInput.trim()
            if (host.isEmpty()) {
                _userMessage.value = getApplication<Application>().getString(R.string.auto_detect_need_host)
                return@launch
            }
            _autoDetectRunning.value = true
            try {
                _userMessage.value = getApplication<Application>().getString(R.string.auto_detect_scanning)
                val result = withContext(Dispatchers.IO) {
                    HaEndpointScanner.findFirstWebSocket(app.okHttpClient, host)
                }
                result.fold(
                    onSuccess = { (sch, port) ->
                        _pendingAutoFill.value = AutoDetectFill(scheme = sch, port = port)
                        _userMessage.value = getApplication<Application>().getString(
                            R.string.auto_detect_success,
                            sch,
                            port,
                        )
                    },
                    onFailure = {
                        _userMessage.value = getApplication<Application>().getString(R.string.auto_detect_failed)
                    },
                )
            } finally {
                _autoDetectRunning.value = false
            }
        }
    }

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

    fun testHaConnection(scheme: String, host: String, portText: String) {
        viewModelScope.launch {
            val port = portText.toIntOrNull()
            if (port == null || port < 1 || port > 65535) {
                _userMessage.value = getApplication<Application>().getString(R.string.test_feedback_port_invalid)
                return@launch
            }
            val url = WebSocketUrlFactory.build(scheme, host, port)
            if (url == null) {
                _userMessage.value = getApplication<Application>().getString(R.string.test_feedback_host_empty)
                return@launch
            }
            _userMessage.value = getApplication<Application>().getString(R.string.test_feedback_connecting)
            val result = withContext(Dispatchers.IO) {
                HaConnectionTester.testWebSocket(app.okHttpClient, url)
            }
            _userMessage.value = result.fold(
                onSuccess = {
                    getApplication<Application>().getString(R.string.test_feedback_ws_ok)
                },
                onFailure = { t ->
                    getApplication<Application>().getString(
                        R.string.test_feedback_ws_fail,
                        t.message ?: t.javaClass.simpleName,
                    )
                },
            )
        }
    }

    fun testNetworkTts(networkBaseUrl: String, networkApiKey: String) {
        val root = networkBaseUrl.trim()
        if (root.isEmpty()) {
            _userMessage.value = getApplication<Application>().getString(R.string.test_feedback_tts_url_empty)
            return
        }
        _userMessage.value = getApplication<Application>().getString(R.string.test_feedback_tts_testing)
        val sample = getApplication<Application>().getString(R.string.network_tts_test_phrase)
        app.ttsManager.testNetworkTts(root, networkApiKey.trim(), sample) { success, detail ->
            _userMessage.value = if (success) {
                getApplication<Application>().getString(R.string.test_feedback_tts_ok)
            } else {
                if (detail == "play_or_http_failed") {
                    getApplication<Application>().getString(R.string.test_feedback_tts_fail_generic)
                } else {
                    getApplication<Application>().getString(R.string.test_feedback_tts_fail_detail, detail)
                }
            }
        }
    }

    companion object {
        fun factory(app: HerdroidApplication): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                        return SettingsViewModel(app, app.settingsRepository, app) as T
                    }
                    throw IllegalArgumentException()
                }
            }
        }
    }
}
