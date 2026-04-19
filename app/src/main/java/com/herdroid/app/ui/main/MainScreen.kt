@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
)

package com.herdroid.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.herdroid.app.R
import com.herdroid.app.data.settings.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userMessage by viewModel.userMessage.collectAsState()
    val volumeWarning by viewModel.volumeWarning.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences.DEFAULT)
    val cdAutoPlayTts = stringResource(R.string.cd_auto_play_tts)

    val chatTimeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val rootView = LocalView.current

    LaunchedEffect(Unit) {
        ViewCompat.requestApplyInsets(rootView.rootView)
    }

    LaunchedEffect(
        chatMessages.size,
        chatMessages.lastOrNull()?.id,
        chatMessages.lastOrNull()?.text,
    ) {
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

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_title)) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.read_aloud_reply_label),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Switch(
                            checked = prefs.autoPlayTts,
                            onCheckedChange = viewModel::setAutoPlayTts,
                            modifier = Modifier.semantics {
                                contentDescription = cdAutoPlayTts
                            },
                        )
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.cd_open_settings))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        // 整列施加 IME / 导航栏 insets，使底部输入区随键盘上移（勿用 Scaffold.bottomBar，Insets 易不生效）
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                    itemsIndexed(
                        chatMessages,
                        key = { _, msg -> msg.id },
                    ) { index, msg ->
                        val prev = chatMessages.getOrNull(index - 1)
                        val showTimestamp = prev == null ||
                            (msg.timeMillis - prev.timeMillis) >= CHAT_TIMESTAMP_GAP_MS
                        val showStreamingPlaceholder =
                            msg.role == ChatMessageRole.Assistant && msg.text.isEmpty()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (showTimestamp) {
                                Text(
                                    text = "[${chatTimeFormatter.format(Date(msg.timeMillis))}]",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            ChatBubble(
                                message = msg,
                                showStreamingPlaceholder = showStreamingPlaceholder,
                                autoPlayTts = prefs.autoPlayTts,
                                onReadAloud = { viewModel.readMessageAloud(msg.text) },
                                onResend = { viewModel.sendChatMessage(msg.text) },
                                onCopy = { viewModel.copyMessageToClipboard(msg.text) },
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { viewModel.clearChat() },
                    enabled = chatMessages.isNotEmpty(),
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
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    minLines = 1,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendChatMessage(inputText)
                                inputText = ""
                                focusManager.clearFocus()
                            }
                        },
                    ),
                )
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
    }

        volumeWarning?.let { text ->
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                    .zIndex(1f),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatUiMessage,
    showStreamingPlaceholder: Boolean = false,
    autoPlayTts: Boolean = false,
    onReadAloud: () -> Unit,
    onResend: () -> Unit,
    onCopy: () -> Unit,
) {
    val isUser = message.role == ChatMessageRole.User
    val bodyText = if (!isUser && message.text.isEmpty() && showStreamingPlaceholder) {
        stringResource(R.string.chat_streaming_placeholder)
    } else {
        message.text
    }
    var menuExpanded by remember(message.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (isUser) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.End,
            ) {
                MessageBubbleWithMenu(
                    message = message,
                    displayText = bodyText,
                    showStreamingPlaceholder = showStreamingPlaceholder,
                    assistantAutoFold = false,
                    menuExpanded = menuExpanded,
                    onMenuExpandedChange = { menuExpanded = it },
                    onReadAloud = onReadAloud,
                    onResend = onResend,
                    onCopy = onCopy,
                    isUser = true,
                )
                Spacer(modifier = Modifier.width(4.dp))
                UserSendStateIcon(state = message.userSendState)
            }
        } else {
            MessageBubbleWithMenu(
                message = message,
                displayText = bodyText,
                showStreamingPlaceholder = showStreamingPlaceholder,
                assistantAutoFold = autoPlayTts,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onReadAloud = onReadAloud,
                onResend = onResend,
                onCopy = onCopy,
                isUser = false,
            )
        }
    }
}

@Composable
private fun MessageBubbleWithMenu(
    message: ChatUiMessage,
    displayText: String,
    showStreamingPlaceholder: Boolean,
    assistantAutoFold: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onReadAloud: () -> Unit,
    onResend: () -> Unit,
    onCopy: () -> Unit,
    isUser: Boolean,
) {
    val shouldFold =
        !isUser &&
            assistantAutoFold &&
            message.replyComplete &&
            message.text.isNotEmpty() &&
            !showStreamingPlaceholder

    val cdAssistantCollapsed = stringResource(R.string.cd_assistant_reply_collapsed)

    var replyExpanded by remember(message.id) { mutableStateOf(true) }
    var didApplyInitialCollapse by remember(message.id) { mutableStateOf(false) }

    LaunchedEffect(message.replyComplete, assistantAutoFold, message.id) {
        if (isUser || !message.replyComplete) return@LaunchedEffect
        if (!assistantAutoFold) {
            replyExpanded = true
            return@LaunchedEffect
        }
        if (!didApplyInitialCollapse) {
            replyExpanded = false
            didApplyInitialCollapse = true
        }
    }

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier
                .widthIn(max = 320.dp)
                .then(
                    if (shouldFold) {
                        Modifier.combinedClickable(
                            onClick = {
                                if (!replyExpanded) {
                                    replyExpanded = true
                                }
                            },
                            onLongClick = { onMenuExpandedChange(true) },
                        )
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { onMenuExpandedChange(true) },
                            )
                        }
                    },
                ),
        ) {
            when {
                shouldFold && !replyExpanded -> {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics {
                                contentDescription = cdAssistantCollapsed
                            },
                        )
                        TextButton(
                            onClick = { replyExpanded = true },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.chat_expand))
                        }
                    }
                }
                shouldFold && replyExpanded -> {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = message.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(
                            onClick = { replyExpanded = false },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(stringResource(R.string.chat_collapse))
                        }
                    }
                }
                else -> {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (displayText != message.text) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuExpandedChange(false) },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_menu_read_aloud)) },
                onClick = {
                    onMenuExpandedChange(false)
                    onReadAloud()
                },
            )
            if (isUser) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_menu_resend)) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onResend()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_menu_copy)) },
                    onClick = {
                        onMenuExpandedChange(false)
                        onCopy()
                    },
                )
            }
        }
    }
}

@Composable
private fun UserSendStateIcon(state: UserMessageSendState?) {
    when (state) {
        UserMessageSendState.Sending -> {
            Spacer(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(18.dp),
            )
        }
        UserMessageSendState.Sent -> {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.cd_message_send_sent),
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        UserMessageSendState.Failed -> {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.cd_message_send_failed),
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        null -> {}
    }
}
