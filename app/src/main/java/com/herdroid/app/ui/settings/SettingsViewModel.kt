package com.herdroid.app.ui.settings

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.herdroid.app.HerdroidApplication
import com.herdroid.app.R
import com.herdroid.app.data.chat.OpenAiChatClient
import com.herdroid.app.data.settings.SettingsRepository
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

    private val chatClient = OpenAiChatClient(app.okHttpClient)

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val appCtx: Application get() = getApplication()

    /** 无活动网络时（如飞行模式）提前提示；纯局域网服务在已连 Wi‑Fi 时通常仍有 INTERNET 能力位。 */
    private fun hasActiveNetwork(): Boolean {
        val cm = appCtx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun save(
        scheme: String,
        host: String,
        port: Int,
        apiBaseUrl: String,
        apiKey: String,
        modelName: String,
    ) {
        viewModelScope.launch {
            repository.update(
                scheme = scheme,
                host = host,
                port = port,
                apiBaseUrl = apiBaseUrl,
                apiKey = apiKey,
                modelName = modelName,
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
            if (!hasActiveNetwork()) {
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

    fun testChatCompletion(apiBaseUrl: String, apiKey: String, model: String) {
        val root = apiBaseUrl.trim()
        if (root.isEmpty()) {
            _userMessage.value = appCtx.getString(R.string.chat_need_base_url)
            return
        }
        if (apiKey.isBlank()) {
            _userMessage.value = appCtx.getString(R.string.chat_need_api_key)
            return
        }
        viewModelScope.launch {
            if (!hasActiveNetwork()) {
                _userMessage.value = appCtx.getString(R.string.test_feedback_network_unavailable)
                return@launch
            }
            _userMessage.value = appCtx.getString(R.string.test_chat_testing)
            val result = withContext(Dispatchers.IO) {
                chatClient.chatCompletions(
                    baseUrl = root,
                    apiKey = apiKey.trim(),
                    model = model.trim().ifEmpty { "hermes-agent" },
                    messages = listOf("user" to DEFAULT_TEST_CHAT_PROMPT),
                )
            }
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
