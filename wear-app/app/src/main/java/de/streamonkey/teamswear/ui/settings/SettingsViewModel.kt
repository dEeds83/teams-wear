package de.streamonkey.teamswear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.streamonkey.teamswear.auth.AuthRepository
import de.streamonkey.teamswear.data.RelayRepository
import de.streamonkey.teamswear.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val relay: RelayRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val pushEnabled: StateFlow<Boolean> =
        settings.pushEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** True, wenn ein Relay konfiguriert ist (sonst Toggle ausblenden). */
    val pushAvailable: Boolean get() = relay.pushEnabled

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun setPush(enabled: Boolean) {
        viewModelScope.launch {
            _busy.value = true
            try {
                settings.setPushEnabled(enabled)
                if (enabled) relay.registerCurrentDevice() else relay.unregister()
            } catch (_: Exception) {
                // Netzfehler ignorieren; Renewal/erneuter Toggle holt es nach.
            } finally {
                _busy.value = false
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { relay.unregister() }
            auth.logout()
            onDone()
        }
    }
}
