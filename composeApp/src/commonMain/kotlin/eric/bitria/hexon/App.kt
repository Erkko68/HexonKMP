package eric.bitria.hexon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import eric.bitria.hexon.ui.elements.GameScreen
import eric.bitria.hexon.viewmodel.GameViewModel

@Composable
fun App(
    viewModel: GameViewModel = remember { GameViewModel() }
) {
    val gameEvents = viewModel.gameEvents.collectAsState(initial = "Waiting for events...")

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            GameScreen(
                communication = viewModel.gameCommunication,
                modifier = Modifier.fillMaxSize()
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
                        viewModel.sendCommand("""{
  "type": "INIT_BOARD",
  "config": {
    "radius": 2,
    "tiles": [
      {
        "type": "forest",
        "position": { "q": 0, "r": 0 },
        "token": 5
      },
      {
        "type": "hills",
        "position": { "q": 1, "r": 0 },
        "token": 2
      },
      {
        "type": "pasture",
        "position": { "q": 0, "r": -1 },
        "token": 10
      },
      {
        "type": "desert",
        "position": { "q": -1, "r": 0 },
        "token": null
      },
      {
        "type": "fields",
        "position": { "q": -1, "r": 1 },
        "token": 3
      },
      {
        "type": "mountains",
        "position": { "q": 1, "r": -1 },
        "token": 6
      },
      {
        "type": "fields",
        "position": { "q": 0, "r": 1 },
        "token": 8
      }
    ]
  }
}""".trimIndent())
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Start Game", color = Color.White)
                }
                Text("Last event: ${gameEvents.value}", color = Color.White)
            }
        }
    }
}