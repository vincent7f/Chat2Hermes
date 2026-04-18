package com.herdroid.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herdroid.app.R
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.TtsEngineType
import com.herdroid.app.data.settings.UserPreferences

private val SCHEME_OPTIONS = listOf("ws", "wss", "http", "https")

private fun normalizeScheme(value: String): String {
    val lower = value.lowercase().trim()
    return if (lower in SCHEME_OPTIONS) lower else "http"
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
    // Use collectAsState (not lifecycle-aware) so NavHost destinations always see Scanning/AskingUser
    // updates immediately; collectAsStateWithLifecycle can miss emissions tied to back stack lifecycle.
    val autoDetectUi by viewModel.autoDetectUi.collectAsState()
    val pendingAutoFill by viewModel.pendingAutoFill.collectAsStateWithLifecycle()

    LaunchedEffect(userMessage) {
        val m = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m)
        viewModel.clearUserMessage()
    }

    LaunchedEffect(pendingAutoFill) {
        val fill = pendingAutoFill ?: return@LaunchedEffect
        scheme = normalizeScheme(fill.scheme)
        portText = fill.port.toString()
        portError = null
        viewModel.consumePendingAutoFill()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
            // Use Surface(onClick) instead of overlay on OutlinedTextField: in verticalScroll + unbounded
            // height, overlay match was wrong and the clickable layer could get 0 height.
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
                onClick = { viewModel.autoDetect(host) },
                enabled = autoDetectUi is AutoDetectUiState.Idle && host.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auto_detect))
            }
            Text(
                text = stringResource(R.string.auto_detect_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    viewModel.testHaConnection(scheme, host, portText)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_connection))
            }
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
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(stringResource(R.string.network_tts_api_key)) },
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

        when (val ui = autoDetectUi) {
            is AutoDetectUiState.Scanning -> {
                Dialog(onDismissRequest = { viewModel.cancelAutoDetect() }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(R.string.auto_detect_progress_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val denom = ui.total.coerceAtLeast(1)
                            val fraction =
                                (ui.currentIndex.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.auto_detect_progress_body,
                                    ui.currentIndex,
                                    ui.total,
                                    ui.scheme,
                                    ui.port,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { viewModel.cancelAutoDetect() }) {
                                    Text(stringResource(R.string.auto_detect_interrupt))
                                }
                            }
                        }
                    }
                }
            }

            is AutoDetectUiState.AskingUser -> {
                Dialog(onDismissRequest = { viewModel.cancelAutoDetect() }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 3.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Text(
                                text = stringResource(R.string.auto_detect_ask_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(
                                    R.string.auto_detect_ask_message,
                                    ui.scheme,
                                    ui.port,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { viewModel.onAutoDetectSkipFound() }) {
                                    Text(stringResource(R.string.auto_detect_skip_continue))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.onAutoDetectAcceptFound() }) {
                                    Text(stringResource(R.string.auto_detect_use_confirm))
                                }
                            }
                        }
                    }
                }
            }

            else -> {}
        }
    }
}
