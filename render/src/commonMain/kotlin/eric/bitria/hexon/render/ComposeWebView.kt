package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A simplified WebView wrapper for Kotlin Multiplatform.
 * Optimized for loading local HTML data and using JS Bridge.
 */
@Composable
fun ComposeWebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
    controller: WebViewController = rememberWebViewController(),
    javaScriptInterfaces: Map<String, Any> = emptyMap(),
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
    onConsoleMessage: (ConsoleMessage) -> Unit = {},
    jsBridge: WebViewJsBridge? = null,
) {
    ComposeWebViewImpl(
        state = state,
        modifier = modifier,
        controller = controller,
        javaScriptInterfaces = javaScriptInterfaces,
        onCreated = onCreated,
        onDispose = onDispose,
        onConsoleMessage = onConsoleMessage,
        jsBridge = jsBridge,
    )
}

@Composable
internal expect fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    controller: WebViewController,
    javaScriptInterfaces: Map<String, Any>,
    onCreated: (WebView) -> Unit,
    onDispose: (WebView) -> Unit,
    onConsoleMessage: (ConsoleMessage) -> Unit,
    jsBridge: WebViewJsBridge?,
)
