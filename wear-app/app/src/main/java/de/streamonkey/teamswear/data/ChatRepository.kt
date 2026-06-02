package de.streamonkey.teamswear.data

import de.streamonkey.teamswear.graph.Chat
import de.streamonkey.teamswear.graph.ChatMessage
import de.streamonkey.teamswear.graph.GraphApi
import de.streamonkey.teamswear.graph.ItemBody
import de.streamonkey.teamswear.graph.SendMessageBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Einzige Datenquelle fuer Chats/Nachrichten. Spricht Graph direkt und mappt
 * auf UI-Modelle. Haelt die eigene User-ID gecached fuer isMine-Erkennung.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val graph: GraphApi,
    private val cache: ChatCache,
) {
    private var myUserId: String? = null
    private val meMutex = Mutex()

    private suspend fun myId(): String {
        myUserId?.let { return it }
        return meMutex.withLock {
            myUserId ?: graph.me().id.also { myUserId = it }
        }
    }

    /** Sofort verfuegbarer Cache (kann leer sein). */
    suspend fun cachedChats(): List<ChatSummary> = cache.loadChats()

    /** Frische Chat-Liste vom Server; aktualisiert den Cache. */
    suspend fun refreshChats(): List<ChatSummary> {
        val chats = graph.chats().value.map { it.toSummary() }
        cache.saveChats(chats)
        return chats
    }

    /**
     * Liefert die Nachrichten eines Chats, aelteste zuerst.
     * System-Events und leere Bodies werden herausgefiltert; Graph liefert
     * neueste zuerst, daher wird nach [MessageItem.timestamp] aufsteigend sortiert.
     */
    suspend fun messages(chatId: String): List<MessageItem> {
        val me = myId()
        return graph.messages(chatId).value
            .filter { it.messageType == null || it.messageType == "message" }
            .filter { it.body.content.isNotBlank() }
            .map { it.toItem(me) }
            .sortedBy { it.timestamp }
    }

    /** Sendet [text] als Textnachricht und gibt die neu erstellte Nachricht zurueck. */
    suspend fun send(chatId: String, text: String): MessageItem {
        val sent = graph.sendMessage(chatId, SendMessageBody(ItemBody("text", text)))
        return sent.toItem(myId())
    }

    private fun Chat.toSummary(): ChatSummary {
        val title = topic
            ?: members.mapNotNull { it.displayName }.filter { it.isNotBlank() }
                .joinToString(", ").ifBlank { "Chat" }
        val preview = lastMessagePreview?.body?.content?.let { stripHtml(it) } ?: ""
        return ChatSummary(
            id = id,
            title = title,
            preview = preview,
            timestamp = lastMessagePreview?.createdDateTime ?: lastUpdatedDateTime,
        )
    }

    private fun ChatMessage.toItem(myId: String): MessageItem {
        val raw = if (body.contentType == "html") stripHtml(body.content) else body.content
        return MessageItem(
            id = id,
            author = from?.user?.displayName ?: "",
            text = raw,
            timestamp = createdDateTime,
            isMine = from?.user?.id == myId,
        )
    }

    /** Sehr einfacher HTML-Stripper fuer Teams-Nachrichten-Bodies. */
    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()
}
