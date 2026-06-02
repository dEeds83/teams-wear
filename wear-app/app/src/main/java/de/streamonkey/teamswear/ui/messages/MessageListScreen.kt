package de.streamonkey.teamswear.ui.messages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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

    // Beim Laden (und nach dem Senden) ans Ende springen: neueste Nachricht unten.
    // Index = Header(0) + alle Nachrichten -> letzte Nachricht liegt bei size.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.size)
        }
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
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        if (msg.text.isNotBlank()) {
            val colors = if (msg.isMine) ChipDefaults.primaryChipColors()
            else ChipDefaults.secondaryChipColors()
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
        msg.imageUrls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .padding(top = 2.dp),
            )
        }
    }
}
