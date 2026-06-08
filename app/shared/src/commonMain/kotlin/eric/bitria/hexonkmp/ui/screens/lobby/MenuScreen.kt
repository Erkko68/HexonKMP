package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.components.sheets.NameDialog
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import org.koin.compose.viewmodel.koinViewModel

// The main menu: choose a name, then Find Game (matchmaking) or open the Private
// Lobby dialog (create / join by code). Shares the LobbyViewModel with the lobby
// screen, so the connection it starts carries over; [onEnterLobby] navigates to the
// waiting room once a connection is established.
@Composable
fun MenuScreen(
    onEnterLobby: () -> Unit,
    viewModel: LobbyViewModel = koinViewModel(),
) {
    val playerName by viewModel.playerName.collectAsStateWithLifecycle()
    val joinError by viewModel.joinError.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.enterLobby.collect { onEnterLobby() }
    }

    var editingName by remember { mutableStateOf(false) }
    var showPrivate by remember { mutableStateOf(false) }
    val ready = playerName != null

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
                PlayerToken(MaterialTheme.colorScheme.primary, playerName!!, size = Tokens.tokenSm)
                Text(playerName!!, style = MaterialTheme.typography.titleSmall)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Hexon", style = MaterialTheme.typography.displayMedium)
            Text("Ready to play?", style = MaterialTheme.typography.bodyLarge)
            Button(onClick = viewModel::findGame, enabled = ready, modifier = Modifier.widthIn(min = 200.dp)) {
                Text("Find Game")
            }
            Button(
                onClick = { viewModel.clearJoinError(); showPrivate = true },
                enabled = ready,
                modifier = Modifier.widthIn(min = 200.dp),
            ) { Text("Private Lobby") }
        }

        if (playerName == null || editingName) {
            NameDialog(
                initial = playerName.orEmpty(),
                onConfirm = { viewModel.submitName(it); editingName = false },
                onDismiss = if (playerName == null) null else ({ editingName = false }),
            )
        }

        if (showPrivate) {
            PrivateLobbyDialog(
                error = joinError,
                onJoin = viewModel::joinLobby,
                onCreate = viewModel::createLobby,
                onDismiss = { showPrivate = false; viewModel.clearJoinError() },
            )
        }
    }
}
