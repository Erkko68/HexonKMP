package eric.bitria.hexon.ui.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import eric.bitria.hexon.communication.GameCommunication
import eric.bitria.hexon.render.WebViewGameRender

/**
 * Game screen UI component using WebView for all platforms.
 * Handles rendering of the game content while delegating communication to GameCommunication.
 *
 * @param communication The communication handler for JSON exchange
 * @param modifier Compose modifier for layout customization
 */
@Composable
fun GameScreen(
    communication: GameCommunication,
    modifier: Modifier = Modifier
) {
    val webViewRender = WebViewGameRender()

    // Connect communication with WebView
    LaunchedEffect(communication) {
        communication.setSendJsonHandler { json ->
            webViewRender.sendJson(json)
        }

        // Register a callback to handle incoming JSON messages from the WebView
        webViewRender.registerJsonCallback { json ->
            communication.handleReceivedJson(json)
        }
    }

    // Render the WebView
    webViewRender.Render(modifier)
}