package de.streamonkey.teamswear.ui.util

import android.app.RemoteInput
import android.content.Intent
import androidx.wear.input.RemoteInputIntentHelper

// Muss in NotificationPublisher (RemoteInput-Builder), ReplyReceiver und buildReplyIntent gleich sein.
const val REPLY_KEY = "teams_wear_reply"

/** Standard-Quick-Replies fuer den Chooser. */
val QUICK_REPLIES = arrayOf("👍", "Ok", "Danke!", "Bin gleich da", "Melde mich später")

/**
 * Baut den Wear-RemoteInput-Intent: bietet Voice-to-Text, Tastatur UND
 * Quick-Replies in einem Chooser an. Free-Form-Input erlaubt auch Emojis.
 */
fun buildReplyIntent(): Intent {
    val remoteInputs = listOf(
        RemoteInput.Builder(REPLY_KEY)
            .setLabel("Antworten")
            .setChoices(QUICK_REPLIES)
            .setAllowFreeFormInput(true)
            .build()
    )
    return RemoteInputIntentHelper.createActionRemoteInputIntent().apply {
        RemoteInputIntentHelper.putRemoteInputsExtra(this, remoteInputs)
    }
}

/** Liest die Antwort aus dem Activity-Result-Intent. */
fun extractReply(data: Intent?): String? {
    val intent = data ?: return null
    val results = RemoteInput.getResultsFromIntent(intent) ?: return null
    return results.getCharSequence(REPLY_KEY)?.toString()?.trim()?.ifBlank { null }
}
