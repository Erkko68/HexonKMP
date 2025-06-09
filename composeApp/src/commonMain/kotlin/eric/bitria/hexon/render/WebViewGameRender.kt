package eric.bitria.hexon.render

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
 * WebView-based implementation of [GameRender] used to render game content via HTML/JavaScript.
 *
 * This class creates a bidirectional JSON communication channel between Kotlin and the embedded
 * WebView. Messages from JavaScript are dispatched to registered suspend callbacks, while commands
 * from Kotlin are sent to the JS context via an exposed interface.
 */
class WebViewGameRender : GameRender {

    /**
     * A list of registered suspend callbacks to be invoked when a message is received from JavaScript.
     */
    private val jsonCallbacks = mutableListOf<suspend (String) -> Unit>()

    /**
     * Reference to the WebView's navigation controller for executing JavaScript and navigation actions.
     */
    private var navigator: WebViewNavigator? = null

    /**
     * Indicates whether the WebView content has finished loading.
     */
    private var isLoaded = false

    /**
     * Coroutine scope tied to the main dispatcher for managing UI-bound coroutines.
     */
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    /**
     * Composable function that sets up and renders the WebView with bundled HTML and JS.
     * Also registers a JS bridge handler for incoming messages.
     *
     * @param modifier A [Modifier] for layout customization.
     */
    @Composable
    override fun render(modifier: Modifier) {
        var html by remember { mutableStateOf("") }
        var js by remember { mutableStateOf("") }

        // Load HTML and JS content from bundled resources.
        LaunchedEffect(Unit) {
            html = Res.readBytes("files/index.html").decodeToString()
            js = Res.readBytes("files/bundle.js").decodeToString()
        }

        // Initialize the WebView state with injected JS code.
        val webViewState = rememberWebViewStateWithHTMLData(
            data = html.replace("/*CODE*/", js)
        )

        val currentNavigator = rememberWebViewNavigator()
        val bridge = rememberWebViewJsBridge()

        // Register a JS message handler to dispatch events to Kotlin callbacks.
        bridge.register(object : IJsMessageHandler {
            override fun handle(
                message: JsMessage,
                navigator: WebViewNavigator?,
                callback: (String) -> Unit
            ) {
                coroutineScope.launch {
                    jsonCallbacks.forEach { cb -> cb(message.params) }
                }
            }

            override fun methodName() = "GameEvent"
        })

        // Store the navigator reference for later use.
        navigator = currentNavigator

        // Track WebView loading state to enable JS communication only after content is ready.
        LaunchedEffect(webViewState.loadingState) {
            isLoaded = webViewState.loadingState is LoadingState.Finished
        }

        // Render the actual WebView.
        WebView(
            modifier = modifier,
            state = webViewState,
            navigator = currentNavigator,
            webViewJsBridge = bridge
        )
    }

    /**
     * Sends a JSON-encoded message to the JavaScript context inside the WebView.
     *
     * @param json The JSON string to send to JavaScript.
     */
    override suspend fun sendJson(json: String) {
        if (isLoaded) {
            navigator?.evaluateJavaScript("window.receiveFromApp('${json.escapeForJS()}')")
        }
    }

    /**
     * Registers a callback to be invoked when a message is received from JavaScript.
     *
     * @param callback A suspend function to handle the JSON payload from JS.
     */
    override fun receiveJson(callback: suspend (String) -> Unit) {
        jsonCallbacks.add(callback)
    }

    /**
     * Escapes a [String] to be safely embedded inside a JavaScript string literal.
     *
     * @receiver The raw string to escape.
     * @return The escaped JavaScript-safe string.
     */
    private fun String.escapeForJS(): String = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
}
