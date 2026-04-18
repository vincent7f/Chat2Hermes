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
import com.herdroid.app.domain.HealthCheckUrlFactory
import com.herdroid.app.domain.WebSocketUrlFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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

    private val _autoDetectUi = MutableStateFlow<AutoDetectUiState>(AutoDetectUiState.Idle)
    val autoDetectUi: StateFlow<AutoDetectUiState> = _autoDetectUi.asStateFlow()

    private val _pendingAutoFill = MutableStateFlow<AutoDetectFill?>(null)
    val pendingAutoFill: StateFlow<AutoDetectFill?> = _pendingAutoFill.asStateFlow()

    private var autoDetectJob: Job? = null
    private var confirmDeferred: CompletableDeferred<Boolean>? = null

    private val appCtx: Application get() = getApplication()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun consumePendingAutoFill() {
        _pendingAutoFill.value = null
    }

    fun cancelAutoDetect() {
        confirmDeferred?.cancel(CancellationException("cancelled"))
        confirmDeferred = null
        autoDetectJob?.cancel()
        autoDetectJob = null
        if (_autoDetectUi.value !is AutoDetectUiState.Idle) {
            _autoDetectUi.value = AutoDetectUiState.Idle
        }
    }

    /** User chose to apply the found scheme/port. */
    fun onAutoDetectAcceptFound() {
        confirmDeferred?.complete(true)
    }

    /** User chose to keep scanning for another endpoint. */
    fun onAutoDetectSkipFound() {
        confirmDeferred?.complete(false)
    }

    fun autoDetect(hostInput: String, apiKey: String) {
        val host = hostInput.trim()
        if (host.isEmpty()) {
            _userMessage.value = appCtx.getString(R.string.auto_detect_need_host)
            return
        }
        cancelAutoDetect()
        val bearer = apiKey.trim()
        autoDetectJob = viewModelScope.launch {
            val steps = HaEndpointScanner.scanSteps(host)
            val total = steps.size
            try {
                for ((stepIndex, step) in steps.withIndex()) {
                    ensureActive()
                    val index = stepIndex + 1
                    withContext(Dispatchers.Main.immediate) {
                        _autoDetectUi.value = AutoDetectUiState.Scanning(
                            currentIndex = index,
                            total = total,
                            scheme = step.displayScheme,
                            port = step.port,
                        )
                    }
                    val result = withContext(Dispatchers.IO) {
                        HaConnectionTester.testWebSocket(
                            app.okHttpClient,
                            step.probeUrl,
                            HaEndpointScanner.DEFAULT_TIMEOUT_MS,
                            bearerToken = bearer,
                        )
                    }
                    if (result.isFailure) continue

                    val d = CompletableDeferred<Boolean>()
                    confirmDeferred = d
                    withContext(Dispatchers.Main.immediate) {
                        _autoDetectUi.value = AutoDetectUiState.AskingUser(
                            scheme = step.displayScheme,
                            port = step.port,
                        )
                    }
                    val accepted = try {
                        d.await()
                    } catch (e: CancellationException) {
                        throw e
                    } finally {
                        confirmDeferred = null
                    }
                    if (accepted) {
                        _pendingAutoFill.value = AutoDetectFill(
                            scheme = step.displayScheme,
                            port = step.port,
                        )
                        _userMessage.value = appCtx.getString(
                            R.string.auto_detect_applied,
                            step.displayScheme,
                            step.port,
                        )
                        _autoDetectUi.value = AutoDetectUiState.Idle
                        return@launch
                    }
                }
                _userMessage.value = appCtx.getString(R.string.auto_detect_failed)
            } catch (e: CancellationException) {
                // user hit interrupt; stay silent or short message
            } finally {
                autoDetectJob = null
                confirmDeferred = null
                withContext(Dispatchers.Main.immediate) {
                    if (_autoDetectUi.value !is AutoDetectUiState.Idle) {
                        _autoDetectUi.value = AutoDetectUiState.Idle
                    }
                }
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
        networkTtsModel: String,
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
                networkTtsModel = networkTtsModel,
            )
        }
    }

    fun testHaConnection(scheme: String, host: String, portText: String, apiKey: String) {
        viewModelScope.launch {
            val port = portText.toIntOrNull()
            if (port == null || port < 1 || port > 65535) {
                _userMessage.value = appCtx.getString(R.string.test_feedback_port_invalid)
                return@launch
            }
            val healthUrl = HealthCheckUrlFactory.build(scheme, host, port)
            if (healthUrl == null) {
                _userMessage.value = appCtx.getString(R.string.test_feedback_host_empty)
                return@launch
            }
            _userMessage.value = appCtx.getString(R.string.test_feedback_connecting)
            val result = withContext(Dispatchers.IO) {
                HaConnectionTester.testHttpGet(
                    app.okHttpClient,
                    healthUrl,
                    bearerToken = apiKey.trim(),
                )
            }
            _userMessage.value = result.fold(
                onSuccess = {
                    appCtx.getString(R.string.test_feedback_health_ok, healthUrl)
                },
                onFailure = { t ->
                    appCtx.getString(
                        R.string.test_feedback_health_fail,
                        healthUrl,
                        t.message ?: t.javaClass.simpleName,
                    )
                },
            )
        }
    }

    fun testNetworkTts(networkBaseUrl: String, networkApiKey: String, networkModel: String) {
        val root = networkBaseUrl.trim()
        if (root.isEmpty()) {
            _userMessage.value = appCtx.getString(R.string.test_feedback_tts_url_empty)
            return
        }
        _userMessage.value = appCtx.getString(R.string.test_feedback_tts_testing)
        val sample = appCtx.getString(R.string.network_tts_test_phrase)
        app.ttsManager.testNetworkTts(root, networkApiKey.trim(), networkModel, sample) { success, detail ->
            _userMessage.value = if (success) {
                appCtx.getString(R.string.test_feedback_tts_ok)
            } else {
                if (detail == "play_or_http_failed") {
                    appCtx.getString(R.string.test_feedback_tts_fail_generic)
                } else {
                    appCtx.getString(R.string.test_feedback_tts_fail_detail, detail)
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
