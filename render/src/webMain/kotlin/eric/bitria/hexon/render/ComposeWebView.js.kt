package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.WebElementView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLIFrameElement

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    jsBridge: WebViewJsBridge?,
) {
    // 1. Create the WebView wrapper state
    val webView = remember { WebView() }

    // 2. Render the Iframe
    WebElementView(
        // IMPORTANT: The drawBehind modifier "punches a hole" in the canvas
        modifier = modifier.drawBehind {
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
        },
        factory = {
            val iframeElement = (document.createElement("iframe") as HTMLIFrameElement).apply {
                style.apply {
                    border = "none"
                    width = "100%"
                    height = "100%"
                }

                // Pushing the iframe container behind the Compose Canvas
                window.requestAnimationFrame {
                    (parentElement as? HTMLElement)?.style?.zIndex = "-1"
                }
            }

            webView.iframe = iframeElement
            state.webView = webView
            iframeElement
        },
        update = { }
    )

    // 3. Lifecycle & Script Injection
    LaunchedEffect(state.content, jsBridge, state.webView) {
        val content = state.content as? WebContent.Data ?: return@LaunchedEffect
        val currentIframe = state.webView?.iframe ?: return@LaunchedEffect

        currentIframe.onload = {
            try {
                // Step A: Initialize the Bridge (Host -> Client)
                jsBridge?.attach(webView)

                // Step B: Boot the Content (Client -> Host)
                val contentWindow = currentIframe.contentWindow
                if (contentWindow != null) {
                    val jsWindow = contentWindow.asDynamic()

                    // Check if the function exists to avoid crashing on blank/error pages
                    if (jsWindow.bootHexon != undefined) {
                        jsWindow.bootHexon()
                    } else {
                        console.error("Hexon WebView: bootHexon() not found in iframe.")
                    }
                }

                state.loadingState = LoadingState.Finished
            } catch (e: Throwable) {
                console.error("Hexon WebView: Error during iframe initialization", e)
            }
            Unit
        }

        // 4. Inject the HTML (This triggers the load)
        currentIframe.srcdoc = wrapScriptInHtml(
            content.data,
            jsBridge?.jsScript
        )
    }

    // 5. Cleanup
    DisposableEffect(Unit) {
        onDispose {
            state.webView = null
            webView.iframe?.onload = null
            webView.iframe = null
        }
    }
}

/**
 * Wraps the user script in a 'bootloader' function.
 * The script will NOT execute until bootHexon() is called from Kotlin.
 */
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
            // 1. Inject Bridge Definitions (Synchronous)
            ${bridgeScript ?: ""}
        </script>
    </head>
    <body>
        <canvas id="three-root"></canvas>
        <script>
            // 2. Define the Bootloader
            // This function encapsulates the user logic.
            window.bootHexon = function() {
                try {
                    // Execute User Script
                    $script
                } catch (e) {
                    console.error("Hexon Content Error:", e);
                }
            };
        </script>
    </body>
    </html>
""".trimIndent()