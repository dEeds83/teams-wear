package de.streamonkey.teamswear.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val pushEnabled by viewModel.pushEnabled.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
    ) {
        item { ListHeader { Text("Einstellungen") } }

        if (viewModel.pushAvailable) {
            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = pushEnabled,
                    enabled = !busy,
                    onCheckedChange = { viewModel.setPush(it) },
                    label = { Text("Push-Benachrichtigungen") },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(pushEnabled),
                            contentDescription = if (pushEnabled) "An" else "Aus",
                        )
                    },
                )
            }
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.logout(onLoggedOut) },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text("Abmelden") },
            )
        }
    }
}
