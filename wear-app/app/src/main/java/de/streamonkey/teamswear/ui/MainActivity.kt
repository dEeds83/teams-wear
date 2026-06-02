package de.streamonkey.teamswear.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import de.streamonkey.teamswear.notifications.EXTRA_CHAT_ID
import de.streamonkey.teamswear.notifications.EXTRA_CHAT_TITLE

/** Ziel-Chat aus einem Notification-Tap. */
data class ChatTarget(val chatId: String, val title: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* egal */ }

    // Wird beim Start / onNewIntent aus dem Notification-Tap gefuellt.
    private var pendingChat by mutableStateOf<ChatTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        pendingChat = parseChatTarget(intent)
        setContent {
            TeamsWearApp(
                pendingChat = pendingChat,
                onChatConsumed = { pendingChat = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseChatTarget(intent)?.let { pendingChat = it }
    }

    private fun parseChatTarget(intent: Intent?): ChatTarget? {
        val chatId = intent?.getStringExtra(EXTRA_CHAT_ID) ?: return null
        val title = intent.getStringExtra(EXTRA_CHAT_TITLE) ?: "Chat"
        return ChatTarget(chatId, title)
    }

    /** Ab API 33 ist POST_NOTIFICATIONS eine Runtime-Permission (fuer FCM-Push). */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
