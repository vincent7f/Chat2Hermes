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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herdroid.app.R
import kotlinx.coroutines.launch
import com.herdroid.app.data.settings.SettingsRepository
import com.herdroid.app.data.settings.UserPreferences
import com.herdroid.app.domain.HealthCheckUrlFactory

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
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var portError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(prefs) {
        scheme = normalizeScheme(prefs.scheme)
        host = prefs.host
        portText = prefs.port.toString()
        apiKey = prefs.apiKey
        modelName = prefs.modelName
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val saveScope = rememberCoroutineScope()
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
            val chatApiPreview = remember(scheme, host, portText) {
                val p = portText.toIntOrNull()
                if (p == null || p < 1 || p > 65535) null
                else HealthCheckUrlFactory.buildHttpOrigin(normalizeScheme(scheme), host, p)
            }
            Text(
                text = if (chatApiPreview != null) {
                    stringResource(R.string.chat_api_root_preview, chatApiPreview)
                } else {
                    stringResource(R.string.chat_api_root_preview_invalid)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text(stringResource(R.string.model_name_label)) },
                placeholder = { Text(stringResource(R.string.hint_model_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedButton(
                onClick = {
                    viewModel.testChatCompletion(scheme, host, portText, apiKey, modelName)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_chat))
            }
            Text(
                text = stringResource(R.string.test_chat_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = {
                    val port = portText.toIntOrNull()
                    if (port == null || port < 1 || port > 65535) {
                        portError = portInvalidText
                        return@Button
                    }
                    saveScope.launch {
                        viewModel.save(
                            scheme = scheme,
                            host = host,
                            port = port,
                            apiKey = apiKey,
                            modelName = modelName,
                        )
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}
