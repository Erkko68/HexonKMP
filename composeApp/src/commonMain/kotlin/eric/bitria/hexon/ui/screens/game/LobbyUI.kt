package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.viewmodel.game.LobbyViewModel

@Composable
fun LobbyUI(
    onExitClicked: () -> Unit,
    viewModel: LobbyViewModel
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Lobby: Waiting for players...", style = MaterialTheme.typography.headlineMedium)
    }
}