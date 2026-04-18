package com.herdroid.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.herdroid.app.R
import com.herdroid.app.data.ha.HaConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val lastMessage by viewModel.lastMessage.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        val msg = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearUserMessage()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_open_settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ConnectionStatusChip(connectionState)
            Text(
                text = stringResource(R.string.last_message_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = lastMessage.ifEmpty { stringResource(R.string.no_message_yet) },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auto_tts))
                Switch(
                    checked = preferences.autoPlayTts,
                    onCheckedChange = { viewModel.setAutoPlay(it) },
                )
            }
            Button(
                onClick = { viewModel.reconnect() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reconnect))
            }
        }
    }
}

@Composable
private fun ConnectionStatusChip(state: HaConnectionState) {
    val (label, enabled) = when (state) {
        HaConnectionState.Disconnected -> stringResource(R.string.status_disconnected) to false
        HaConnectionState.Connecting -> stringResource(R.string.status_connecting) to false
        HaConnectionState.Connected -> stringResource(R.string.status_connected) to true
        is HaConnectionState.Error -> stringResource(R.string.status_error, state.message) to false
    }
    FilterChip(
        selected = enabled,
        onClick = {},
        enabled = false,
        label = { Text(label) },
    )
}
