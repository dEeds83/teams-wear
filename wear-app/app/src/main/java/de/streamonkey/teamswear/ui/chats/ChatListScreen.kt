package de.streamonkey.teamswear.ui.chats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import de.streamonkey.teamswear.data.ChatSummary
import de.streamonkey.teamswear.ui.util.relativeTime

@Composable
fun ChatListScreen(
    onOpenChat: (chatId: String, title: String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item { ListHeader { Text("Chats") } }

        if (state.chats.isEmpty() && !state.loading) {
            item {
                Text(
                    state.error ?: "Keine Chats",
                    style = MaterialTheme.typography.caption1,
                )
            }
        }

        items(state.chats, key = { it.id }) { chat ->
            ChatRow(chat) { onOpenChat(chat.id, chat.title) }
        }

        item {
            CompactChip(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenSettings,
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text("Einstellungen") },
            )
        }
    }
}

@Composable
private fun ChatRow(chat: ChatSummary, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = ChipDefaults.secondaryChipColors(),
        label = {
            Text(chat.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        secondaryLabel = {
            val time = relativeTime(chat.timestamp)
            val sub = if (chat.preview.isNotBlank()) chat.preview else time
            Text(sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    )
}
