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
    jsBridge: WebViewJsBridge? = null,
) {
    ComposeWebViewImpl(
        state = state,
        modifier = modifier,
        jsBridge = jsBridge,
    )
}

@Composable
internal expect fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    jsBridge: WebViewJsBridge?,
)
