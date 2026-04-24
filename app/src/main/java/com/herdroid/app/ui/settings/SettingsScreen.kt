package com.herdroid.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
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
private val RUNS_RECONNECT_OPTIONS = listOf(0, 1, 3, 6, 10)

private fun normalizeScheme(value: String): String {
    val lower = value.lowercase().trim()
    if (lower in SCHEME_OPTIONS) return lower
    return when (lower) {
        "ws" -> "http"
        "wss" -> "https"
        else -> "http"
    }
}

private fun normalizeRunsReconnectAttempts(value: Int): Int {
    if (value in RUNS_RECONNECT_OPTIONS) return value
    return 3
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
    val context = LocalContext.current
    val portInvalidText = stringResource(R.string.port_invalid)

    var scheme by remember { mutableStateOf(normalizeScheme(prefs.scheme)) }
    var schemePickerVisible by remember { mutableStateOf(false) }
    var host by remember { mutableStateOf(prefs.host) }
    var portText by remember { mutableStateOf(prefs.port.toString()) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var modelName by remember { mutableStateOf(prefs.modelName) }
    var runsReconnectAttempts by remember {
        mutableStateOf(normalizeRunsReconnectAttempts(prefs.runsAutoReconnectAttempts))
    }
    var runsReconnectMenuExpanded by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf<String?>(null) }

    val activeProfileId by repository.activeProfileId.collectAsStateWithLifecycle(initialValue = "default")
    val profileIds by repository.profileIds.collectAsStateWithLifecycle(initialValue = listOf("default"))
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var addProfileDialogVisible by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var deleteProfileDialogVisible by remember { mutableStateOf(false) }
    var deleteProfileFinalConfirmVisible by remember { mutableStateOf(false) }

    LaunchedEffect(prefs) {
        scheme = normalizeScheme(prefs.scheme)
        host = prefs.host
        portText = prefs.port.toString()
        apiKey = prefs.apiKey
        modelName = prefs.modelName
        runsReconnectAttempts = normalizeRunsReconnectAttempts(prefs.runsAutoReconnectAttempts)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val saveScope = rememberCoroutineScope()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val isTestingChat by viewModel.isTestingChat.collectAsStateWithLifecycle()
    var profileSwitchTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userMessage) {
        val m = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(m)
        viewModel.clearUserMessage()
    }
    LaunchedEffect(activeProfileId) {
        val target = profileSwitchTarget ?: return@LaunchedEffect
        if (target == activeProfileId) {
            snackbarHostState.showSnackbar(
                context.getString(R.string.settings_profile_switched_to, activeProfileId),
            )
            profileSwitchTarget = null
        }
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
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { profileMenuExpanded = true },
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
                                text = stringResource(R.string.settings_profile_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = activeProfileId,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(
                    expanded = profileMenuExpanded,
                    onDismissRequest = { profileMenuExpanded = false },
                ) {
                    profileIds.forEach { id ->
                        DropdownMenuItem(
                            text = { Text(id) },
                            onClick = {
                                profileMenuExpanded = false
                                if (id != activeProfileId) {
                                    profileSwitchTarget = id
                                    viewModel.switchProfile(id)
                                }
                            },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        newProfileName = ""
                        addProfileDialogVisible = true
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_profile_add))
                }
                OutlinedButton(
                    onClick = { deleteProfileDialogVisible = true },
                    enabled = profileIds.size > 1,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.settings_profile_delete))
                }
            }
            if (addProfileDialogVisible) {
                AlertDialog(
                    onDismissRequest = { addProfileDialogVisible = false },
                    title = { Text(stringResource(R.string.settings_profile_add)) },
                    text = {
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text(stringResource(R.string.settings_profile_add_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val name = newProfileName
                                addProfileDialogVisible = false
                                newProfileName = ""
                                viewModel.addProfile(name) { ok ->
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (ok) {
                                                context.getString(R.string.settings_profile_add_success)
                                            } else {
                                                context.getString(R.string.settings_profile_add_failed)
                                            },
                                        )
                                    }
                                }
                            },
                        ) {
                            Text(stringResource(R.string.settings_profile_add_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { addProfileDialogVisible = false }) {
                            Text(stringResource(R.string.settings_profile_add_cancel))
                        }
                    },
                )
            }
            if (deleteProfileDialogVisible) {
                AlertDialog(
                    onDismissRequest = { deleteProfileDialogVisible = false },
                    title = { Text(stringResource(R.string.settings_profile_delete)) },
                    text = {
                        Text(
                            stringResource(
                                R.string.settings_profile_delete_confirm,
                                activeProfileId,
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                deleteProfileDialogVisible = false
                                deleteProfileFinalConfirmVisible = true
                            },
                        ) {
                            Text(stringResource(R.string.settings_profile_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteProfileDialogVisible = false }) {
                            Text(stringResource(R.string.settings_profile_add_cancel))
                        }
                    },
                )
            }
            if (deleteProfileFinalConfirmVisible) {
                AlertDialog(
                    onDismissRequest = { deleteProfileFinalConfirmVisible = false },
                    title = { Text(stringResource(R.string.settings_profile_delete_final_title)) },
                    text = { Text(stringResource(R.string.settings_profile_delete_irreversible)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val deletingId = activeProfileId
                                deleteProfileFinalConfirmVisible = false
                                viewModel.deleteProfile(deletingId) { ok ->
                                    snackbarScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (ok) {
                                                context.getString(R.string.settings_profile_delete_success, deletingId)
                                            } else {
                                                context.getString(R.string.settings_profile_delete_failed)
                                            },
                                        )
                                    }
                                }
                            },
                        ) {
                            Text(stringResource(R.string.settings_profile_delete_confirm_final))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteProfileFinalConfirmVisible = false }) {
                            Text(stringResource(R.string.settings_profile_add_cancel))
                        }
                    },
                )
            }
            Text(
                text = stringResource(R.string.settings_section_connection),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    onClick = { schemePickerVisible = true },
                    modifier = Modifier.weight(1f),
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
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(stringResource(R.string.host_label)) },
                    placeholder = { Text(stringResource(R.string.hint_host)) },
                    modifier = Modifier.weight(1.4f),
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
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = portError != null,
                    supportingText = {
                        if (portError != null) {
                            Text(portError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                )
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
            Text(
                text = stringResource(R.string.settings_section_model_auth),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(stringResource(R.string.hermes_api_key)) },
                    placeholder = { Text(stringResource(R.string.hermes_api_key_hint)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(stringResource(R.string.model_name_label)) },
                    placeholder = { Text(stringResource(R.string.hint_model_name)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            Text(
                text = stringResource(R.string.hermes_api_key_supporting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.settings_section_stability),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { runsReconnectMenuExpanded = true },
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
                                text = stringResource(R.string.runs_reconnect_attempts_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = runsReconnectAttempts.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                DropdownMenu(
                    expanded = runsReconnectMenuExpanded,
                    onDismissRequest = { runsReconnectMenuExpanded = false },
                ) {
                    RUNS_RECONNECT_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.toString()) },
                            onClick = {
                                runsReconnectAttempts = option
                                runsReconnectMenuExpanded = false
                            },
                        )
                    }
                }
            }
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
                            runsAutoReconnectAttempts = runsReconnectAttempts,
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

    if (isTestingChat) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.test_chat)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text(stringResource(R.string.test_chat_testing))
                }
            },
            confirmButton = {},
        )
    }
}
