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
                is GameCommand.DiceRolled -> bridge.emit("diceRolled", command)
                is GameCommand.RobberUpdated -> bridge.emit("robberUpdated", command)
                is GameCommand.PlaceBuilding -> bridge.emit("placeBuilding", command)
                is GameCommand.SetHex -> bridge.emit("setHex", command)
                is GameCommand.SetPort -> bridge.emit("setPort", command)
            }
        }
    }

    ComposeWebView(
        state = state,
        modifier = modifier,
        jsBridge = bridge
    )
}