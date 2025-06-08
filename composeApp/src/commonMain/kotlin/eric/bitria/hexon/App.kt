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
import eric.bitria.hexon.viewmodel.GameViewModel

@Composable
fun App() {
    val viewModel = remember { GameViewModel() }
    val gameEvents = viewModel.gameEvents.collectAsState(initial = "Hola tet")

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Pure rendering - no logic
            viewModel.gameRender.Render(Modifier.fillMaxSize())

            // UI Layer
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        viewModel.sendCommand("""{"type": "updateCubeScale","scale": {"x": 1.5,"y": 2.0,"z": 1.0}}""".trimIndent())
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