package com.herdroid.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
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
    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatLoading by viewModel.chatLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(chatMessages.size, chatMessages.lastOrNull()?.id) {
        if (chatMessages.isNotEmpty()) {
            val last = chatMessages.lastIndex
            if (last >= 0) {
                listState.scrollToItem(last)
            }
        }
    }

    LaunchedEffect(userMessage) {
        val msg = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearUserMessage()
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.scrollToItem(chatMessages.lastIndex)
        }
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            ConnectionStatusChip(connectionState)
            Text(
                text = stringResource(R.string.push_message_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = lastMessage.ifEmpty { stringResource(R.string.no_message_yet) },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )
            HorizontalDivider()
            Text(
                text = stringResource(R.string.chat_section_title),
                style = MaterialTheme.typography.labelLarge,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (chatMessages.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(chatMessages, key = { it.id }) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { viewModel.clearChat() },
                    enabled = chatMessages.isNotEmpty() && !chatLoading,
                ) {
                    Text(stringResource(R.string.chat_clear))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    enabled = !chatLoading,
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !chatLoading) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                    ),
                )
                if (chatLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(40.dp),
                    )
                } else {
                    IconButton(
                        onClick = {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                            focusManager.clearFocus()
                        },
                        enabled = inputText.isNotBlank(),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_chat),
                        )
                    }
                }
            }
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
private fun ChatBubble(message: ChatUiMessage) {
    val isUser = message.role == ChatMessageRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
            )
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
