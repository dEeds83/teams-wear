package de.streamonkey.teamswear.ui.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import de.streamonkey.teamswear.data.MessageItem
import de.streamonkey.teamswear.ui.util.buildReplyIntent
import de.streamonkey.teamswear.ui.util.extractReply

@Composable
fun MessageListScreen(
    title: String,
    viewModel: MessageListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    // RemoteInput-Chooser: Voice + Tastatur + Quick-Replies.
    val replyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        extractReply(result.data)?.let { viewModel.send(it) }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item { ListHeader { Text(title, maxLines = 1) } }

        items(state.messages, key = { it.id }) { msg -> MessageBubble(msg) }

        if (state.messages.isEmpty() && !state.loading) {
            item { Text(state.error ?: "Keine Nachrichten", style = MaterialTheme.typography.caption1) }
        }

        item {
            CompactChip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { replyLauncher.launch(buildReplyIntent()) },
                label = {
                    Text(if (state.sending) "Senden…" else "Antworten", textAlign = TextAlign.Center)
                },
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}

@Composable
private fun MessageBubble(msg: MessageItem) {
    val colors = if (msg.isMine) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors()
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = {},
        colors = colors,
        label = { Text(msg.text) },
        secondaryLabel = if (!msg.isMine && msg.author.isNotBlank()) {
            { Text(msg.author, maxLines = 1) }
        } else null,
    )
}
