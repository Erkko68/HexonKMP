package eric.bitria.hexon.ui.screens.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexon.ui.theme.HexonTheme
import eric.bitria.hexon.viewmodel.game.MatchmakingViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MatchmakingUI(
    onExitClicked: () -> Unit,
    onGameStarted: () -> Unit,
    viewModel: MatchmakingViewModel = koinViewModel()
) {
    if (viewModel.navigateToGameplay.value) {
        LaunchedEffect(Unit) {
            onGameStarted()
        }
    }

    val spacing = HexonTheme.dimensions.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.screenHorizontal, spacing.screenVertical),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = viewModel.statusMessage,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            Text(
                text = "Waiting players... ${viewModel.playersFound}/${viewModel.maxPlayers}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Button(
                onClick = {
                    viewModel.leaveMatchmaking()
                    onExitClicked()
                }
            ) {
                Text("Cancel")
            }
        }
    }
}
