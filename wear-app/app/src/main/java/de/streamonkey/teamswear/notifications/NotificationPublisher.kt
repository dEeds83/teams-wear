package de.streamonkey.teamswear.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import de.streamonkey.teamswear.R
import de.streamonkey.teamswear.ui.MainActivity
import de.streamonkey.teamswear.ui.util.QUICK_REPLIES
import de.streamonkey.teamswear.ui.util.REPLY_KEY

// Gemeinsame Konstanten fuer NotificationPublisher und ReplyReceiver.
const val CHANNEL_ID = "teams_messages"
const val EXTRA_CHAT_ID = "chatId"
const val EXTRA_CHAT_TITLE = "chatTitle"
const val EXTRA_NOTIF_ID = "notifId"

/**
 * Baut eine Wear-Notification fuer eine eingehende Teams-Nachricht — mit
 * Inline-Reply-Action (Voice/Tastatur/Quick-Replies via RemoteInput).
 */
object NotificationPublisher {

    /** Legt den Notification-Channel an, falls noch nicht vorhanden (idempotent). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Teams Nachrichten", NotificationManager.IMPORTANCE_HIGH
        )
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun show(context: Context, chatId: String, sender: String, preview: String) {
        ensureChannel(context)
        val notifId = chatId.hashCode()

        val remoteInput = RemoteInput.Builder(REPLY_KEY)
            .setLabel(context.getString(R.string.reply_label))
            .setChoices(QUICK_REPLIES)
            .setAllowFreeFormInput(true)
            .build()

        val replyIntent = Intent(context, ReplyReceiver::class.java).apply {
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOTIF_ID, notifId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val replyPending = PendingIntent.getBroadcast(context, notifId, replyIntent, flags)

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            context.getString(R.string.reply_label),
            replyPending,
        ).addRemoteInput(remoteInput).build()

        // Tap auf die Notification -> App oeffnet direkt diesen Chat.
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_CHAT_TITLE, sender)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val contentPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(sender)
            .setContentText(preview)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(contentPending)
            .addAction(replyAction)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }
}
