package de.streamonkey.teamswear.graph

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GraphList<T>(
    val value: List<T> = emptyList(),
    @SerialName("@odata.nextLink") val nextLink: String? = null,
)

// --- /me ---
@Serializable
data class GraphUser(
    val id: String,
    val displayName: String? = null,
    val userPrincipalName: String? = null,
)

// --- /me/chats ---
@Serializable
data class Chat(
    val id: String,
    val topic: String? = null,
    @SerialName("chatType") val chatType: String? = null, // oneOnOne | group | meeting
    val lastUpdatedDateTime: String? = null,
    val members: List<ChatMember> = emptyList(),
    @SerialName("lastMessagePreview") val lastMessagePreview: LastMessagePreview? = null,
)

@Serializable
data class ChatMember(
    val id: String? = null,
    val displayName: String? = null,
    @SerialName("userId") val userId: String? = null,
)

@Serializable
data class LastMessagePreview(
    val id: String? = null,
    val createdDateTime: String? = null,
    val body: ItemBody? = null,
)

// --- /chats/{id}/messages ---
@Serializable
data class ChatMessage(
    val id: String,
    val createdDateTime: String? = null,
    val from: MessageFrom? = null,
    val body: ItemBody = ItemBody(),
    @SerialName("messageType") val messageType: String? = null, // message | systemEventMessage
)

@Serializable
data class MessageFrom(
    val user: MessageUser? = null,
)

@Serializable
data class MessageUser(
    val id: String? = null,
    val displayName: String? = null,
)

@Serializable
data class ItemBody(
    val contentType: String = "text", // text | html
    val content: String = "",
)

/** Body fuer POST: eine neue Nachricht senden. */
@Serializable
data class SendMessageBody(
    val body: ItemBody,
)
