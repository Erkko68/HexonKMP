package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

/**
 * Game Layer UI component using WebView for all platforms.
 * Handles rendering of the game content while delegating communication to the provided
 * data source and callback.
 *
 * @param modifier Compose modifier for layout customization
 * @param jsonCollector A Flow emitting JSON strings to be sent to the WebView
 * @param onJsonReceived Callback invoked when a JSON message is received from the WebView
 */

@Composable
fun GameLayer(
    modifier: Modifier = Modifier,
    jsonCollector: Flow<String>,
    onJsonReceived: (String) -> Unit
) {
    val webViewRender = remember { WebViewGameRender() }

    LaunchedEffect(jsonCollector, onJsonReceived) {
        // Register the provided callback
        webViewRender.registerJsonCallback(onJsonReceived)

        // Collect from provided flow and send to WebView
        jsonCollector.collect { json ->
            webViewRender.sendJson(json)
        }
    }

    webViewRender.Render(modifier)
}
