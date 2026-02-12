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
fun ThreeJsView(
    commands: Flow<GameCommand>,
    onRenderEvent: (RenderEvent) -> Unit,
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

        // A. Render Engine Events
        bridge.register<String, Unit>("onEngineReady") { _ ->
            onRenderEvent(RenderEvent.Initialised)
        }

        // B. Game Interaction Events
        bridge.register<GameEvent.PlacedBuilding, Unit>("GameEvent") { event ->
            onGameEvent(event)
        }
    }

    // 3. Handle Commands (Kotlin -> JS)
    LaunchedEffect(Unit) {
        commands.collect { command ->
            when (command) {
                is GameCommand.DiceRolled -> bridge.emit("DiceRolled", command)
                is GameCommand.RobberUpdated -> bridge.emit("RobberUpdated", command)
                is GameCommand.PlaceBuilding -> bridge.emit("PlaceBuilding", command)
                is GameCommand.SetHex -> bridge.emit("SetHex", command)
                is GameCommand.SetPort -> bridge.emit("SetPort", command)
                is GameCommand.ShowEdgeBuildingPositions -> bridge.emit("ShowEdgeBuildingPositions", command)
                is GameCommand.ShowVertexBuildingPositions -> bridge.emit("ShowVertexBuildingPositions", command)
            }
        }
    }

    ComposeWebView(
        state = state,
        modifier = modifier,
        jsBridge = bridge
    )
}