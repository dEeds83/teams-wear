package de.streamonkey.teamswear.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.streamonkey.teamswear.auth.AuthRepository
import de.streamonkey.teamswear.auth.LoginResult
import de.streamonkey.teamswear.data.RelayRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Starting : LoginUiState
    /** Code anzeigen + auf Bestaetigung warten. */
    data class AwaitingCode(val userCode: String, val verificationUri: String) : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/**
 * Steuert den Device-Code-Login-Flow. Nach erfolgreichem Login wird
 * best-effort die Push-Registrierung beim Relay angestossen (kein Fehler bei
 * Phase-1-Betrieb ohne Relay).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val relay: RelayRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    /** Startet den Device-Code-Flow; ignoriert Doppelaufrufe waehrend Starting/AwaitingCode. */
    fun start() {
        if (_state.value is LoginUiState.Starting || _state.value is LoginUiState.AwaitingCode) return
        _state.value = LoginUiState.Starting
        viewModelScope.launch {
            try {
                val dc = auth.beginDeviceCode()
                _state.value = LoginUiState.AwaitingCode(dc.userCode, dc.verificationUri)
                when (val r = auth.pollForToken(dc)) {
                    is LoginResult.Success -> {
                        // Push-Registrierung best-effort (kein Block bei Phase-1-Betrieb).
                        runCatching { relay.registerCurrentDevice() }
                        _state.value = LoginUiState.Success
                    }
                    is LoginResult.Failed -> _state.value = LoginUiState.Error(r.reason)
                }
            } catch (e: Exception) {
                _state.value = LoginUiState.Error(e.message ?: "Login fehlgeschlagen")
            }
        }
    }
}
