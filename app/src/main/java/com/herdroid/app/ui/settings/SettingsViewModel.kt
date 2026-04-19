package com.herdroid.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herdroid.app.HerdroidApplication
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.chat.OpenAiChatFromSettings
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.domain.HaConnectionTester
import com.herdroid.app.domain.HealthCheckUrlFactory
import com.herdroid.app.domain.hasActiveNetwork
import com.herdroid.app.ui.hermes.errorMessage
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

    private val chatClient = app.openAiChatClient

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val appCtx: Application get() = getApplication()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    /** 必须 await 完成后再离开设置页，否则主界面会读到未落盘的旧 API Key。 */
    suspend fun save(
        scheme: String,
        host: String,
        port: Int,
        apiKey: String,
        modelName: String,
    ) {
        repository.update(
            scheme = scheme,
            host = host,
            port = port,
            apiKey = apiKey,
            modelName = modelName,
        )
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
            if (!appCtx.hasActiveNetwork()) {
                _userMessage.value = appCtx.getString(R.string.test_feedback_network_unavailable)
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

    /** 与主界面发消息相同：同一套 URL（[OpenAiChatFromSettings]）与 [executeCompletions]。 */
    fun testChatCompletion(scheme: String, host: String, portText: String, apiKey: String, model: String) {
        viewModelScope.launch {
            val outcome = OpenAiChatFromSettings.prepareFromPortText(scheme, host, portText, apiKey, model)
            outcome.errorMessage(appCtx)?.let {
                _userMessage.value = it
                return@launch
            }
            val prepared = (outcome as OpenAiChatFromSettings.PrepareOutcome.Ready).prepared
            if (!appCtx.hasActiveNetwork()) {
                _userMessage.value = appCtx.getString(R.string.test_feedback_network_unavailable)
                return@launch
            }
            _userMessage.value = appCtx.getString(R.string.test_chat_testing)
            val result = OpenAiChatFromSettings.executeCompletions(
                chatClient,
                prepared,
                listOf("user" to DEFAULT_TEST_CHAT_PROMPT),
            )
            _userMessage.value = result.fold(
                onSuccess = { reply ->
                    appCtx.getString(R.string.test_chat_ok, reply)
                },
                onFailure = { t ->
                    appCtx.getString(
                        R.string.test_chat_fail,
                        t.message ?: t.javaClass.simpleName,
                    )
                },
            )
        }
    }

    companion object {
        private const val DEFAULT_TEST_CHAT_PROMPT = "hi"

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
