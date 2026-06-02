package de.streamonkey.teamswear.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import de.streamonkey.teamswear.data.ChatRepository
import de.streamonkey.teamswear.ui.util.REPLY_KEY
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Empfaengt die Inline-Antwort (RemoteInput) aus einer Teams-Notification und
 * sendet sie via Graph. Holt das ChatRepository ueber einen Hilt-EntryPoint,
 * da BroadcastReceiver kein @AndroidEntryPoint-Field-Injection erlaubt.
 */
class ReplyReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReplyEntryPoint {
        fun chatRepository(): ChatRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(REPLY_KEY)?.toString()?.trim().orEmpty()
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID).orEmpty()
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, chatId.hashCode())
        if (reply.isBlank() || chatId.isBlank()) return

        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, ReplyEntryPoint::class.java)
            .chatRepository()

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repo.send(chatId, reply)
                NotificationManagerCompat.from(context).cancel(notifId)
            } catch (e: Exception) {
                Log.w("ReplyReceiver", "Senden fehlgeschlagen", e)
            } finally {
                pending.finish()
            }
        }
    }
}
