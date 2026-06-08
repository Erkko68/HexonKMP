package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.components.sheets.NameDialog
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

// The lobby / main menu. Drives name selection, matchmaking, and the waiting room;
// when the game starts it signals [onGameStarted] so the host can navigate to the
// game screen. The live connection lives in the shared repository, so navigating
// away doesn't drop it.
@Composable
fun LobbyScreen(
    onGameStarted: () -> Unit,
    viewModel: LobbyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playerName by viewModel.playerName.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.gameStarted.collect { onGameStarted() }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            LobbyUiState.Idle -> IdleContent(
                playerName = playerName,
                onFindGame = viewModel::joinGame,
                onSubmitName = viewModel::submitName,
            )
            LobbyUiState.Connecting -> SearchingContent(
                label = "Connecting…",
                countdownSeconds = null,
                onCancel = viewModel::cancelSearch,
            )
            is LobbyUiState.Waiting -> SearchingContent(
                label = "Waiting for players… ${s.connected} / ${s.needed}",
                countdownSeconds = s.countdownSeconds,
                onCancel = viewModel::cancelSearch,
            )
            is LobbyUiState.Error -> ErrorContent(s.message, onRetry = viewModel::retry)
        }
    }
}

@Composable
private fun IdleContent(
    playerName: String?,
    onFindGame: () -> Unit,
    onSubmitName: (String) -> Unit,
) {
    // The dialog shows on first run (no name yet) or when the player taps their chip
    // to change it.
    var editingName by remember { mutableStateOf(false) }
    val showDialog = playerName == null || editingName

    Box(Modifier.fillMaxSize()) {
        // Identity chip, top-left, once a name has been chosen — tap to rename.
        if (playerName != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.md)
                    .clickable { editingName = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                PlayerToken(
                    color = MaterialTheme.colorScheme.primary,
                    label = playerName,
                    size = Tokens.tokenSm,
                )
                Text(playerName, style = MaterialTheme.typography.titleSmall)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hexon", style = MaterialTheme.typography.displayMedium)
            Text("Ready to play?", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onFindGame, enabled = playerName != null) { Text("Find Game") }
        }

        if (showDialog) {
            NameDialog(
                initial = playerName.orEmpty(),
                onConfirm = { onSubmitName(it); editingName = false },
                // First run requires a name (no cancel); renaming can be dismissed.
                onDismiss = if (playerName == null) null else ({ editingName = false }),
            )
        }
    }
}

@Composable
private fun SearchingContent(
    label: String,
    countdownSeconds: Int?,
    onCancel: () -> Unit,
) {
    // Tick the countdown down locally; re-seed whenever the server sends a new value.
    var remaining by remember(countdownSeconds) { mutableStateOf(countdownSeconds) }
    LaunchedEffect(countdownSeconds) {
        var r = countdownSeconds
        while (r != null && r > 0) {
            delay(1000)
            r -= 1
            remaining = r
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(label, style = MaterialTheme.typography.bodyLarge)
        remaining?.let {
            Text(
                "Starting in ${it}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(
            onClick = onCancel,
            // Inverted accent: dark container with the amber primary as content.
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) { Text("Cancel") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) { Text("Retry") }
    }
}
