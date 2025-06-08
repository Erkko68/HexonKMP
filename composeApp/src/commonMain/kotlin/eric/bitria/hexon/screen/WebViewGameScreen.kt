package eric.bitria.hexon.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import hexonkmp.composeapp.generated.resources.Res
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
* WebView-based implementation of GameScreen.
* Uses a WebView to render game content and establishes a bidirectional JSON communication channel.
*
* @constructor Creates a new WebView game screen instance
*/
class WebViewGameScreen : GameScreen {
    // Callbacks for JSON message reception
    private val jsonCallbacks = mutableListOf<suspend (String) -> Unit>()

    // WebView navigation controller
    private var navigator: WebViewNavigator? = null

    // Tracks if WebView content is loaded
    private var isLoaded = false

    // Coroutine scope for internal operations
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Renders the game content in a WebView.
     * @param modifier Compose modifier for layout customization
     */
    @Composable
    override fun Render(modifier: Modifier) {
        // State for holding HTML content
        var html by remember { mutableStateOf("") }

        // State for holding JavaScript content
        var js by remember { mutableStateOf("") }

        // Load game assets asynchronously
        LaunchedEffect(Unit) {
            html = Res.readBytes("files/index.html").decodeToString()
            js = Res.readBytes("files/bundle.js").decodeToString()
        }

        // WebView state with loaded HTML/JS content
        val webViewState = rememberWebViewStateWithHTMLData(
            data = html.replace("/*CODE*/", js)
        )

        // WebView navigation controller
        val currentNavigator = rememberWebViewNavigator()

        // JavaScript bridge for communication
        val bridge = rememberWebViewJsBridge()

        // Setup message handler when navigator is available
        LaunchedEffect(currentNavigator) {
            navigator = currentNavigator
            bridge.register(object : IJsMessageHandler {
                override fun handle(
                    message: JsMessage,
                    navigator: WebViewNavigator?,
                    callback: (String) -> Unit
                ) {
                    // Dispatch received messages to all registered callbacks
                    coroutineScope.launch {
                        jsonCallbacks.forEach { cb -> cb(message.params) }
                    }
                }

                override fun methodName() = "GameEvent"
            })
        }

        // Track loading state
        LaunchedEffect(webViewState.loadingState) {
            isLoaded = webViewState.loadingState is LoadingState.Finished
        }

        // Actual WebView component
        WebView(
            modifier = modifier,
            state = webViewState,
            navigator = currentNavigator,
            webViewJsBridge = bridge
        )
    }

    /**
     * Sends JSON data to the game.
     * @param json The JSON string to send
     */
    override suspend fun sendJson(json: String) {
        if (isLoaded) {
            navigator?.evaluateJavaScript("window.receiveFromApp('${json.escapeJS()}')")
        }
    }

    /**
     * Registers a callback for receiving JSON from the game.
     * @param callback Function to receive JSON strings
     */
    override fun receiveJson(callback: suspend (String) -> Unit) {
        jsonCallbacks.add(callback)
    }

    /**
     * Escapes special characters in JSON for safe JavaScript evaluation.
     * @return Escaped string safe for JS evaluation
     */
    private fun String.escapeJS(): String = replace("'", "\\'")
}