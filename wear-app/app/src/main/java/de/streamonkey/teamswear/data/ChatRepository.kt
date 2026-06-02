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
        val me = myId()
        val chats = graph.chats().value.map { it.toSummary(me) }
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
            .map { it.toItem(me) }
            // Leere Nachrichten raus, aber Bild-only behalten.
            .filter { it.text.isNotBlank() || it.imageUrls.isNotEmpty() }
            .sortedBy { it.timestamp }
    }

    /** Sendet [text] als Textnachricht und gibt die neu erstellte Nachricht zurueck. */
    suspend fun send(chatId: String, text: String): MessageItem {
        val sent = graph.sendMessage(chatId, SendMessageBody(ItemBody("text", text)))
        return sent.toItem(myId())
    }

    private fun Chat.toSummary(myId: String): ChatSummary {
        // Titel = Gruppenname (topic) oder die anderen Teilnehmer (ohne mich selbst).
        val others = members
            .filter { it.userId != myId }
            .mapNotNull { it.displayName }
            .filter { it.isNotBlank() }
        val title = topic
            ?: others.joinToString(", ").ifBlank {
                // Fallback: Chat nur mit mir selbst -> alle Namen.
                members.mapNotNull { it.displayName }.joinToString(", ").ifBlank { "Chat" }
            }
        val preview = lastMessagePreview?.body?.content?.let { stripHtml(it) } ?: ""
        return ChatSummary(
            id = id,
            title = title,
            preview = preview,
            timestamp = lastMessagePreview?.createdDateTime ?: lastUpdatedDateTime,
        )
    }

    private fun ChatMessage.toItem(myId: String): MessageItem {
        val text: String
        val images: List<String>
        if (body.contentType == "html") {
            images = imgSrcRegex.findAll(body.content).map { it.groupValues[1] }.toList()
            text = stripHtml(body.content)
        } else {
            images = emptyList()
            text = body.content
        }
        return MessageItem(
            id = id,
            author = from?.user?.displayName ?: "",
            text = text,
            timestamp = createdDateTime,
            isMine = from?.user?.id == myId,
            imageUrls = images,
        )
    }

    /** Sehr einfacher HTML-Stripper fuer Teams-Nachrichten-Bodies. */
    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim()

    private companion object {
        // Extrahiert src aus <img ...> (Teams hostedContents-URLs + externe GIFs).
        val imgSrcRegex = Regex("""<img[^>]*\bsrc="([^"]+)"""", RegexOption.IGNORE_CASE)
    }
}
