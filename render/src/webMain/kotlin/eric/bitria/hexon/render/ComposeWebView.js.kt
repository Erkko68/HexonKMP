package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    jsBridge: WebViewJsBridge?,
) {
    // 1. Create the WebView wrapper
    val webView = remember { WebView() }

    // 2. Render the View
    WebElementView(
        modifier = modifier,
        factory = {
            val iframeElement = (document.createElement("iframe") as HTMLIFrameElement).apply {
                style.apply {
                    border = "none"
                    width = "100%"
                    height = "100%"
                    position = "absolute"
                    top = "0px"
                    left = "0px"
                }
            }

            // Assign the nullable variable
            webView.iframe = iframeElement
            state.webView = webView

            iframeElement
        },
        update = { }
    )

    // 3. Lifecycle
    // Added state.webView as a key to ensure we re-run once the iframe is attached
    LaunchedEffect(state.content, jsBridge, state.webView) {
        val content = state.content as? WebContent.Data ?: return@LaunchedEffect

        // Use the iframe from the state to ensure it's available
        val currentIframe = state.webView?.iframe ?: return@LaunchedEffect

        currentIframe.onload = {
            jsBridge?.attach(webView)
            state.loadingState = LoadingState.Finished
            Unit
        }

        currentIframe.srcdoc = wrapScriptInHtml(
            content.data,
            jsBridge?.jsScript
        )
    }

    // 4. Cleanup
    DisposableEffect(Unit) {
        onDispose {
            state.webView = null

            // FIX: Now we just use ?. instead of ::isInitialized
            webView.iframe?.onload = null
            webView.iframe = null // Optional: help GC
        }
    }
}

private fun wrapScriptInHtml(script: String, bridgeScript: String?): String = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            html, body {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                overflow: hidden;
                background: transparent;
            }
            #three-root {
                position: fixed;
                inset: 0;
                display: block;
            }
        </style>
        <script>
            ${bridgeScript ?: ""}
        </script>
    </head>
    <body>
        <canvas id="three-root"></canvas>
        <script>
            $script
        </script>
    </body>
    </html>
""".trimIndent()
