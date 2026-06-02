package de.streamonkey.teamswear.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import de.streamonkey.teamswear.ui.chats.ChatListScreen
import de.streamonkey.teamswear.ui.login.LoginScreen
import de.streamonkey.teamswear.ui.messages.MessageListScreen
import de.streamonkey.teamswear.ui.settings.SettingsScreen

private object Routes {
    const val LOGIN = "login"
    const val CHATS = "chats"
    const val SETTINGS = "settings"
    const val MESSAGES = "messages/{chatId}?title={title}"
    fun messages(chatId: String, title: String) =
        "messages/$chatId?title=${Uri.encode(title)}"
}

@Composable
fun TeamsWearApp(
    pendingChat: ChatTarget? = null,
    onChatConsumed: () -> Unit = {},
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val navController = rememberSwipeDismissableNavController()
    val start = if (rootViewModel.startLoggedIn) Routes.CHATS else Routes.LOGIN

    // Notification-Tap: direkt in den passenden Chat springen (nur wenn eingeloggt).
    LaunchedEffect(pendingChat) {
        val target = pendingChat ?: return@LaunchedEffect
        if (rootViewModel.startLoggedIn) {
            navController.navigate(Routes.messages(target.chatId, target.title))
        }
        onChatConsumed()
    }

    MaterialTheme {
        SwipeDismissableNavHost(navController = navController, startDestination = start) {

            composable(Routes.LOGIN) {
                LoginScreen(onLoggedIn = {
                    navController.navigate(Routes.CHATS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                })
            }

            composable(Routes.CHATS) {
                ChatListScreen(
                    onOpenChat = { chatId, title ->
                        navController.navigate(Routes.messages(chatId, title))
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                })
            }

            composable(
                route = Routes.MESSAGES,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "Chat" },
                ),
            ) { backStackEntry ->
                val title = backStackEntry.arguments?.getString("title") ?: "Chat"
                MessageListScreen(title = title)
            }
        }
    }
}
