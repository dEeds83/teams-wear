package de.streamonkey.teamswear.ui.messages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.streamonkey.teamswear.data.ChatRepository
import de.streamonkey.teamswear.data.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-Zustand des Nachrichtenverlaufs: laufende Lade-/Sende-Anfrage, Nachrichten, Fehlertext. */
data class MessagesUiState(
    val loading: Boolean = true,
    val sending: Boolean = false,
    val messages: List<MessageItem> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val repo: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])

    private val _state = MutableStateFlow(MessagesUiState())
    val state: StateFlow<MessagesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val msgs = repo.messages(chatId)
                _state.value = _state.value.copy(loading = false, messages = msgs)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Fehler")
            }
        }
    }

    fun send(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(sending = true)
            try {
                repo.send(chatId, text)
                // Verlauf neu laden, damit die eigene Nachricht mit Server-ID erscheint.
                val msgs = repo.messages(chatId)
                _state.value = _state.value.copy(sending = false, messages = msgs)
            } catch (e: Exception) {
                _state.value = _state.value.copy(sending = false, error = e.message ?: "Senden fehlgeschlagen")
            }
        }
    }
}
