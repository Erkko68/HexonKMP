package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.GameViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = viewModel { GameViewModel() },
    gameUiViewModel: GameUIViewModel = viewModel { GameUIViewModel() },
) {
    val gameEvents = gameViewModel.gameEvents.collectAsState(initial = "Waiting for events...")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets(0,0,0,0))
    ) {
        GameLayer(
            modifier = Modifier.fillMaxSize(),
            jsonCollector = gameViewModel.sendJson,
            onJsonReceived = gameViewModel::onJsonReceived
        )

        // UI Layer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    gameViewModel.testCommand()
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Start Game", color = Color.White)
            }
            Text("Last event: ${gameEvents.value}", color = Color.Black)
        }
    }
}