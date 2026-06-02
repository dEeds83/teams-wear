package de.streamonkey.teamswear.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import de.streamonkey.teamswear.data.RelayRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Empfaengt FCM-Data-Messages vom Relay und zeigt eine Wear-Notification mit
 * Inline-Reply. Token-Updates werden ans Relay gemeldet.
 */
@AndroidEntryPoint
class TeamsMessagingService : FirebaseMessagingService() {

    @Inject lateinit var relay: RelayRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val chatId = data["chatId"] ?: return
        NotificationPublisher.show(
            context = this,
            chatId = chatId,
            sender = data["sender"] ?: "Teams",
            preview = data["preview"] ?: "",
        )
    }

    override fun onNewToken(token: String) {
        scope.launch {
            runCatching { relay.registerFcmToken(token) }
                .onFailure { Log.w("TeamsFCM", "Relay-Registrierung fehlgeschlagen", it) }
        }
    }
}
