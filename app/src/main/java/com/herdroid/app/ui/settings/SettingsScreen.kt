package com.herdroid.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herdroid.app.R
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.TtsEngineType
import com.herdroid.app.data.settings.UserPreferences

private val SCHEME_OPTIONS = listOf("http", "https")

private fun normalizeScheme(value: String): String {
    val lower = value.lowercase().trim()
    if (lower in SCHEME_OPTIONS) return lower
    return when (lower) {
        "ws" -> "http"
        "wss" -> "https"
        else -> "http"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs by repository.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = UserPreferences.DEFAULT,
    )
    val portInvalidText = stringResource(R.string.port_invalid)

    var scheme by remember { mutableStateOf(normalizeScheme(prefs.scheme)) }
    var schemePickerVisible by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf(prefs.host) }
    var portText by remember { mutableStateOf(prefs.port.toString()) }
    var ttsEngine by remember { mutableStateOf(prefs.ttsEngine) }
    var networkBase by remember { mutableStateOf(prefs.networkTtsBaseUrl) }
    var apiKey by remember { mutableStateOf(prefs.networkTtsApiKey) }
    var networkModel by remember { mutableStateOf(prefs.networkTtsModel) }
    var portError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefs) {
        scheme = normalizeScheme(prefs.scheme)
        host = prefs.host
        portText = prefs.port.toString()
        ttsEngine = prefs.ttsEngine
        networkBase = prefs.networkTtsBaseUrl
        apiKey = prefs.networkTtsApiKey
        networkModel = prefs.networkTtsModel
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        val m = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m)
        viewModel.clearUserMessage()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                onClick = { schemePickerVisible = true },
                modifier = Modifier.fillMaxWidth(),
                shape = OutlinedTextFieldDefaults.shape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                ),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp,
            ) {
                Row(
                    modifier = Modifier
                        .padding(OutlinedTextFieldDefaults.contentPadding())
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.scheme_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = scheme,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.cd_scheme_dropdown),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (schemePickerVisible) {
                AlertDialog(
                    onDismissRequest = { schemePickerVisible = false },
                    title = { Text(stringResource(R.string.scheme_label)) },
                    text = {
                        Column(Modifier.selectableGroup()) {
                            SCHEME_OPTIONS.forEach { option ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .selectable(
                                            selected = option == scheme,
                                            onClick = {
                                                scheme = option
                                                schemePickerVisible = false
                                            },
                                            role = Role.RadioButton,
                                        )
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = option == scheme,
                                        onClick = null,
                                    )
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 8.dp),
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { schemePickerVisible = false }) {
                            Text(stringResource(R.string.scheme_picker_close))
                        }
                    },
                )
            }
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(stringResource(R.string.host_label)) },
                placeholder = { Text(stringResource(R.string.hint_host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = portText,
                onValueChange = {
                    portText = it
                    portError = null
                },
                label = { Text(stringResource(R.string.port_label)) },
                placeholder = { Text(stringResource(R.string.hint_port)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = portError != null,
                supportingText = {
                    if (portError != null) {
                        Text(portError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
            )
            OutlinedButton(
                onClick = {
                    viewModel.testHaConnection(scheme, host, portText, apiKey)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_connection))
            }
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.hermes_api_key)) },
                placeholder = { Text(stringResource(R.string.hermes_api_key_hint)) },
                supportingText = {
                    Text(
                        stringResource(R.string.hermes_api_key_supporting),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = networkModel,
                onValueChange = { networkModel = it },
                label = { Text(stringResource(R.string.network_tts_model)) },
                placeholder = { Text(stringResource(R.string.hint_network_tts_model)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text(stringResource(R.string.tts_engine_label), style = MaterialTheme.typography.labelLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = ttsEngine == TtsEngineType.SYSTEM,
                    onClick = { ttsEngine = TtsEngineType.SYSTEM },
                )
                Text(stringResource(R.string.tts_system))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = ttsEngine == TtsEngineType.NETWORK,
                    onClick = { ttsEngine = TtsEngineType.NETWORK },
                )
                Text(stringResource(R.string.tts_network))
            }
            OutlinedTextField(
                value = networkBase,
                onValueChange = { networkBase = it },
                label = { Text(stringResource(R.string.network_tts_base)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
            )
            OutlinedButton(
                onClick = {
                    viewModel.testNetworkTts(networkBase, apiKey, networkModel)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_network_tts))
            }
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null || port < 1 || port > 65535) {
                        portError = portInvalidText
                        return@Button
                    }
                    viewModel.save(
                        scheme = scheme,
                        host = host,
                        port = port,
                        autoPlayTts = prefs.autoPlayTts,
                        ttsEngine = ttsEngine,
                        networkTtsBaseUrl = networkBase,
                        networkTtsApiKey = apiKey,
                        networkTtsModel = networkModel,
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
