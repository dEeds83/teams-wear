package de.streamonkey.teamswear.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.start() }
    LaunchedEffect(state) { if (state is LoginUiState.Success) onLoggedIn() }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = state) {
            is LoginUiState.AwaitingCode -> {
                Text("Code eingeben auf", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption1)
                Text(s.verificationUri.removePrefix("https://"), textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
                Text(
                    s.userCode,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title1,
                    color = MaterialTheme.colors.primary,
                )
            }
            is LoginUiState.Error -> {
                Text("Fehler", color = MaterialTheme.colors.error, style = MaterialTheme.typography.title3)
                Text(s.message, textAlign = TextAlign.Center, style = MaterialTheme.typography.caption2)
                Button(onClick = { viewModel.start() }) { Text("Erneut") }
            }
            else -> {
                CircularProgressIndicator()
                Text("Anmeldung…", textAlign = TextAlign.Center, style = MaterialTheme.typography.caption1)
            }
        }
    }
}
