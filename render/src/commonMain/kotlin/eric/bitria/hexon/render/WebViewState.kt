package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Sealed class for constraining possible loading states.
 */
sealed class LoadingState {
    data object Initializing : LoadingState()
    data class Loading(val progress: Float) : LoadingState()
    data object Finished : LoadingState()
    data class Failed(val error: WebViewError) : LoadingState()
}

/**
 * State holder for the [ComposeWebView].
 *
 * @property content The content to be loaded into the WebView.
 */
@Stable
class WebViewState(webContent: WebContent) {
    /**
     * The content currently being displayed or to be displayed.
     */
    var content: WebContent by mutableStateOf(webContent)

    /**
     * The current loading state of the WebView.
     */
    var loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)
        internal set

    /**
     * Whether the WebView is currently loading content.
     */
    val isLoading: Boolean
        get() = loadingState !is LoadingState.Finished

    /**
     * A list of errors encountered during the current request.
     */
    var errorsForCurrentRequest: SnapshotStateList<WebViewError> = mutableStateListOf()
        internal set

    /**
     * The underlying [WebView] instance.
     */
    var webView: WebView? by mutableStateOf(null)
        internal set
}

/**
 * Creates and remembers a [WebViewState] for raw data (HTML).
 *
 * @param data The data (HTML) to load.
 * @return A [WebViewState] instance.
 */
@Composable
fun rememberWebViewState(data: String): WebViewState =
    remember { WebViewState(WebContent.Data(data)) }
