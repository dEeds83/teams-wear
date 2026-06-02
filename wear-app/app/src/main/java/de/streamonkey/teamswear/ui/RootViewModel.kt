package de.streamonkey.teamswear.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import de.streamonkey.teamswear.auth.AuthRepository
import javax.inject.Inject

/**
 * Bestimmt beim App-Start einmalig, ob ein gueltiger Login vorliegt,
 * damit [TeamsWearApp] sofort die richtige Start-Destination waehlen kann
 * ohne sichtbares Blitzen zwischen Login- und Chats-Screen.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    auth: AuthRepository,
) : ViewModel() {
    val startLoggedIn: Boolean = auth.isLoggedIn
}
