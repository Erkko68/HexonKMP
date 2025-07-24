package eric.bitria.hexon.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import eric.bitria.hexon.render.WebViewGameRender
import eric.bitria.hexon.viewmodel.GameViewModel

/**
 * Game screen UI component using WebView for all platforms.
 * Handles rendering of the game content while delegating communication to GameViewModel.
 *
 * @param modifier Compose modifier for layout customization
 * @param viewModel The ViewModel that manages game state and communication
 */
@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel
) {
    val webViewRender = remember { WebViewGameRender() }

    LaunchedEffect(viewModel) {
        // Register a callback to handle JSON messages from the WebView
        val callback: (String) -> Unit = { viewModel.onJsonReceived(it) }
        webViewRender.registerJsonCallback(callback)

        // Collect JSON messages from the ViewModel and send them to the WebView
        viewModel.sendJson.collect { json ->
            webViewRender.sendJson(json)
        }
    }

    webViewRender.Render(modifier)
}