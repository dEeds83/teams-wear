package de.streamonkey.teamswear.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("chat_cache")

/**
 * Leichter Offline-Cache fuer die Chat-Liste (DataStore). Damit die Watch nach
 * dem Start sofort etwas anzeigt, bevor der Graph-Call zurueck ist.
 */
@Singleton
class ChatCache @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun saveChats(chats: List<ChatSummary>) {
        context.dataStore.edit { it[KEY_CHATS] = json.encodeToString(chats) }
    }

    suspend fun loadChats(): List<ChatSummary> {
        val raw = context.dataStore.data.first()[KEY_CHATS] ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatSummary>>(raw) }.getOrDefault(emptyList())
    }

    private companion object {
        val KEY_CHATS = stringPreferencesKey("chats_json")
        val json = Json { ignoreUnknownKeys = true }
    }
}
