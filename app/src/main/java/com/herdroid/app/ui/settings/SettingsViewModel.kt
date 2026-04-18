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
import com.herdroid.app.domain.HealthCheckUrlFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val app: HerdroidApplication,
) : AndroidViewModel(application) {

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val appCtx: Application get() = getApplication()

    fun clearUserMessage() {
        _userMessage.value = null
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
