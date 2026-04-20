package com.herdroid.app.ui.main

import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.herdroid.app.R

@Composable
fun MessageSelectionDialog(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    val onSurface = MaterialTheme.colorScheme.onSurface
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                text = stringResource(R.string.chat_select_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxBodyHeight),
                factory = { ctx ->
                    val pad = (16 * ctx.resources.displayMetrics.density).toInt()
                    ScrollView(ctx).apply {
                        isFillViewport = true
                        addView(
                            TextView(ctx).apply {
                                setTextIsSelectable(true)
                                setPadding(pad, pad, pad, pad)
                                textSize = 16f
                            },
                        )
                    }
                },
                update = { scrollView ->
                    val tv = scrollView.getChildAt(0) as TextView
                    tv.text = text
                    tv.setTextColor(onSurface.toArgb())
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_select_dialog_close))
            }
        },
    )
}

@Composable
fun TtsLyricDialog(
    state: TtsLyricUiState,
    onPauseResume: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = LocalConfiguration.current
    val lyricScrollMaxHeight = (config.screenHeightDp * 0.45f).dp
    val lyricScroll = rememberScrollState()
    LaunchedEffect(state.lineIndex) {
        lyricScroll.scrollTo(0)
    }
    Dialog(onDismissRequest = onExit) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.tts_lyric_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = lyricScrollMaxHeight)
                        .verticalScroll(lyricScroll),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = state.previousLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = state.currentLine,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = state.nextLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = stringResource(
                        R.string.tts_lyric_line_progress,
                        state.lineIndex + 1,
                        state.lineCount,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onPauseResume) {
                        Text(
                            text = if (state.isPaused) {
                                stringResource(R.string.tts_lyric_resume)
                            } else {
                                stringResource(R.string.tts_lyric_pause)
                            },
                        )
                    }
                    TextButton(onClick = onExit) {
                        Text(stringResource(R.string.tts_lyric_exit))
                    }
                }
            }
        }
    }
}
