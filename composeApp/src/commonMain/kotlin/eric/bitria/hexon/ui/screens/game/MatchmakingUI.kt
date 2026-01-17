package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.viewmodel.game.MatchmakingViewModel

@Composable
fun MatchmakingUI(
    onExitClicked: () -> Unit,
    viewModel: MatchmakingViewModel
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Searching for a game...", style = MaterialTheme.typography.headlineMedium)
    }
}