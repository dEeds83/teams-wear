package de.streamonkey.teamswear.ui.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.streamonkey.teamswear.data.ChatRepository
import de.streamonkey.teamswear.data.ChatSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-Zustand der Chat-Liste: laufende Anfrage, geladene Chats und optionaler Fehlertext. */
data class ChatListUiState(
    val loading: Boolean = true,
    val chats: List<ChatSummary> = emptyList(),
    val error: String? = null,
)

/**
 * Laedt Chats in zwei Schritten: sofort aus dem Offline-Cache, danach frisch vom Server.
 * Dadurch erscheint die Liste auf der Uhr ohne sichtbare Ladezeit beim Kaltstart.
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repo: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListUiState())
    val state: StateFlow<ChatListUiState> = _state.asStateFlow()

    init { load() }

    /** Laedt zuerst den Cache, dann aktualisiert vom Server; kann manuell erneut aufgerufen werden. */
    fun load() {
        viewModelScope.launch {
            // Cache sofort zeigen.
            val cached = repo.cachedChats()
            if (cached.isNotEmpty()) _state.value = ChatListUiState(loading = true, chats = cached)
            try {
                val fresh = repo.refreshChats()
                _state.value = ChatListUiState(loading = false, chats = fresh)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Fehler")
            }
        }
    }
}
