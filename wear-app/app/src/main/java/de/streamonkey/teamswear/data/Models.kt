package de.streamonkey.teamswear.data

import kotlinx.serialization.Serializable

/** UI-Modell fuer einen Chat in der Liste. */
@Serializable
data class ChatSummary(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: String?, // ISO-8601, fuer relative Anzeige
)

/** UI-Modell fuer eine Nachricht im Verlauf. */
@Serializable
data class MessageItem(
    val id: String,
    val author: String,
    val text: String,
    val timestamp: String?,
    val isMine: Boolean,
)
