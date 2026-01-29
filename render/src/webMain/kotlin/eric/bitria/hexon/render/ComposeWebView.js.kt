package eric.bitria.hexon.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
    val webView = remember { WebView() }

    // Renders the container Box.
    // The external modifier is applied here to handle size and positioning.
    // We use 'drawBehind' with BlendMode.Clear to "punch a hole" in the Compose canvas,
    // allowing the transparent iframe behind it to be visible.
    Box(
        modifier = modifier.drawBehind {
            drawRect(color = Color.Transparent, blendMode = BlendMode.Clear)
        }
    ) {
        WebElementView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                val iframe = (document.createElement("iframe") as HTMLIFrameElement).apply {
                    style.apply {
                        border = "none"
                        width = "100%"
                        height = "100%"
                    }
                    // Visual Hack: Push the iframe explicitly behind the canvas DOM element.
                    // We use requestAnimationFrame to ensure the element is attached before accessing parentElement.
                    window.requestAnimationFrame {
                        (parentElement as? HTMLElement)?.style?.zIndex = "-1"
                    }
                }

                webView.iframe = iframe
                state.webView = webView
                iframe
            },
            update = {
                // No-op: Updates are handled via the srcdoc in LaunchedEffect below.
            }
        )
    }

    // Handles content loading and JS Bridge initialization.
    // This logic creates a strict "Handshake" between Kotlin and JS to prevent race conditions.
    LaunchedEffect(state.content, jsBridge, state.webView) {
        val content = state.content as? WebContent.Data ?: return@LaunchedEffect
        val iframe = state.webView?.iframe ?: return@LaunchedEffect

        // 1. Setup the Handshake
        // We attach the bridge and boot the content only AFTER the DOM is fully parsed.
        iframe.onload = {
            try {
                // Step A: Initialize Host -> Client communication.
                // This injects the native bridge objects (e.g., AppBridgeNative) into the window.
                jsBridge?.attach(webView)

                // Step B: Initialize Client -> Host communication.
                // We manually invoke the 'bootHexon' function defined in the HTML wrapper.
                // This ensures the user script never runs before Step A is complete.
                val contentWindow = iframe.contentWindow
                if (contentWindow != null) {
                    val jsWindow = contentWindow.asDynamic()

                    if (jsWindow.bootHexon != undefined) {
                        jsWindow.bootHexon()
                    } else {
                        console.error("Hexon WebView: bootHexon() function missing in iframe.")
                    }
                }

                state.loadingState = LoadingState.Finished
            } catch (e: Throwable) {
                console.error("Hexon WebView: Initialization failed", e)
            }
            Unit
        }

        // 2. Load Content
        // We inject the HTML, which triggers the 'onload' event defined above.
        iframe.srcdoc = wrapScriptInHtml(content.data, jsBridge?.jsScript)
    }

    DisposableEffect(Unit) {
        onDispose {
            state.webView = null
            webView.iframe?.onload = null
            webView.iframe = null
        }
    }
}

/**
 * Wraps the user script in a secure HTML structure.
 * * Key Architecture:
 * 1. The 'bridgeScript' is placed in <head> to be available immediately.
 * 2. The 'script' (user content) is wrapped in a global function 'window.bootHexon'.
 * This prevents the script from auto-executing until the Kotlin side explicitly calls it.
 */
private fun wrapScriptInHtml(script: String, bridgeScript: String?): String = """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
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
            window.bootHexon = function() {
                try {
                    $script
                } catch (e) {
                    console.error("Hexon Content Error:", e);
                }
            };
        </script>
    </body>
    </html>
""".trimIndent()