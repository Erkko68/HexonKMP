package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import hexonkmp.composeapp.generated.resources.Res
import kotlinx.coroutines.flow.Flow

@Composable
fun HexonGameView(
    commands: Flow<GameCommand>,
    onGameEvent: (GameEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var jsData by remember { mutableStateOf<String?>(null) }

    // 1. Load JS
    LaunchedEffect(Unit) {
        try {
            jsData = Res.readBytes("files/threeJs.js").decodeToString()
        } catch (e: Exception) {
            println("Error loading JS: ${e.message}")
        }
    }

    if (jsData == null) return

    val bridge = rememberWebViewJsBridge()
    val state = rememberWebViewState(data = jsData!!)

    // 2. Register Events (JS -> Kotlin)
    LaunchedEffect(bridge) {

        // A. The Initialization Signal
        bridge.register<String, Unit>("onEngineReady") { _ ->
            onGameEvent(GameEvent.Initialised)
        }
    }

    // 3. Handle Commands (Kotlin -> JS)
    LaunchedEffect(commands) {
        commands.collect { command ->
            when (command) {
                is GameCommand.UpdateSpeed -> bridge.emit("updateSpeed", command)
                is GameCommand.MoveCamera -> bridge.emit("moveCamera", command)
            }
        }
    }

    ComposeWebView(
        state = state,
        modifier = modifier,
        jsBridge = bridge
    )
}