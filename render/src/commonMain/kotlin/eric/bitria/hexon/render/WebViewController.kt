package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Controller for the [ComposeWebView].
 * Simplified to only support HTML loading and JavaScript evaluation.
 */
@Stable
class WebViewController(private val coroutineScope: CoroutineScope) {
    private sealed interface NavigationEvent {
        data class LoadHtml(
            val html: String,
            val baseUrl: String? = null,
            val mimeType: String? = null,
            val encoding: String? = "utf-8",
            val historyUrl: String? = null,
        ) : NavigationEvent

        data class EvaluateJavascript(
            val script: String,
            val callback: ((String) -> Unit)?,
        ) : NavigationEvent
    }

    private val navigationEvents = MutableSharedFlow<NavigationEvent>(replay = 1)

    /**
     * Loads the given HTML content.
     */
    fun loadHtml(
        html: String,
        baseUrl: String? = null,
        mimeType: String? = "text/html",
        encoding: String? = "utf-8",
        historyUrl: String? = null,
    ) {
        coroutineScope.launch {
            navigationEvents.emit(
                NavigationEvent.LoadHtml(
                    html,
                    baseUrl,
                    mimeType,
                    encoding,
                    historyUrl,
                ),
            )
        }
    }

    /**
     * Evaluates the given JavaScript.
     */
    fun evaluateJavascript(
        script: String,
        callback: ((String) -> Unit)? = null,
    ) {
        coroutineScope.launch {
            navigationEvents.emit(NavigationEvent.EvaluateJavascript(script, callback))
        }
    }

    internal suspend fun handleNavigationEvents(webView: WebView) {
        withContext(Dispatchers.Main) {
            navigationEvents.collect { event ->
                when (event) {
                    is NavigationEvent.LoadHtml ->
                        webView.platformLoadDataWithBaseURL(
                            event.baseUrl,
                            event.html,
                            event.mimeType,
                            event.encoding,
                            event.historyUrl,
                        )
                    is NavigationEvent.EvaluateJavascript -> webView.platformEvaluateJavascript(event.script, event.callback)
                }
            }
        }
    }
}

/**
 * Creates and remembers a [WebViewController].
 */
@Composable
fun rememberWebViewController(coroutineScope: CoroutineScope = rememberCoroutineScope()): WebViewController =
    remember(coroutineScope) {
        WebViewController(coroutineScope)
    }
