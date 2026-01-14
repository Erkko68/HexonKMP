package eric.bitria.hexon.render

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.events.Event

@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    controller: WebViewController,
    javaScriptInterfaces: Map<String, Any>,
    onCreated: (WebView) -> Unit,
    onDispose: (WebView) -> Unit,
    onConsoleMessage: (ConsoleMessage) -> Unit,
    jsBridge: WebViewJsBridge?,
) {
    val webView = remember {
        eric.bitria.hexon.render.WebView(document.createElement("div") as HTMLElement)
    }

    val contentInjectionId = "content-injection-point"
    val bridgeInjectionId = "bridge-injection-point"
    val styleInjectionId = "style-injection-point"
    val canvasId = "three-root"

    DisposableEffect(Unit) {
        state.webView = webView
        onCreated(webView)

        onDispose {
            state.webView = null
            document.getElementById(contentInjectionId)?.remove()
            document.getElementById(bridgeInjectionId)?.remove()
            document.getElementById(styleInjectionId)?.remove()
            document.getElementById(canvasId)?.remove()
            onDispose(webView)
        }
    }

    /* ---------- BRIDGE ---------- */
    LaunchedEffect(jsBridge) {
        if (jsBridge != null) {
            jsBridge.attach(webView)
            document.getElementById(bridgeInjectionId)?.remove()

            val script = document.createElement("script") as HTMLScriptElement
            script.id = bridgeInjectionId
            script.type = "text/javascript"
            script.text = jsBridge.jsScript

            document.head?.appendChild(script)
        }
    }

    /* ---------- CONTENT ---------- */
    LaunchedEffect(state.content) {
        val content = state.content
        if (content is WebContent.Data) {
            if (document.getElementById(contentInjectionId) != null) return@LaunchedEffect

            val isFullHtml = content.data.trim().startsWith("<!DOCTYPE") || content.data.trim().startsWith("<html")

            if (isFullHtml) {
                // 1. Extract and Inject Styles
                val styleRegex = Regex("<style>([\\s\\S]*?)</style>")
                val styleMatch = styleRegex.find(content.data)
                if (styleMatch != null) {
                    val style = document.createElement("style")
                    style.id = styleInjectionId
                    // Ensure the background canvas is behind Compose
                    style.textContent = styleMatch.groupValues[1] + "\n#$canvasId { z-index: -1 !important; }"
                    document.head?.appendChild(style)
                }

                // 2. Inject Canvas if missing
                if (content.data.contains("id=\"$canvasId\"") && document.getElementById(canvasId) == null) {
                    val canvas = document.createElement("canvas")
                    canvas.id = canvasId
                    document.body?.appendChild(canvas)
                }
            }

            // 3. Extract and Inject Script
            val scriptData = if (isFullHtml) {
                val regex = Regex("<script>([\\s\\S]*?)</script>")
                val matches = regex.findAll(content.data).toList()
                if (matches.isNotEmpty()) {
                    matches.last().groupValues[1]
                } else {
                    content.data
                }
            } else {
                content.data
            }

            val script = document.createElement("script") as HTMLScriptElement
            script.id = contentInjectionId
            script.type = "text/javascript"
            script.text = scriptData

            document.body?.appendChild(script)
            state.loadingState = LoadingState.Finished
        }
    }

    /* ---------- EXTRA JS INTERFACES ---------- */
    LaunchedEffect(javaScriptInterfaces) {
        javaScriptInterfaces.forEach { (name, obj) ->
            window.asDynamic()[name] = obj
        }
    }

    /* ---------- CONSOLE ---------- */
    DisposableEffect(onConsoleMessage) {
        val listener: (Event) -> Unit = { event ->
            val data = event.asDynamic().data
            if (data?.type == "console") {
                val level = when (data.level as? String) {
                    "DEBUG" -> ConsoleMessageLevel.DEBUG
                    "WARN" -> ConsoleMessageLevel.WARNING
                    "ERROR" -> ConsoleMessageLevel.ERROR
                    else -> ConsoleMessageLevel.LOG
                }
                onConsoleMessage(ConsoleMessage(data.message ?: "", level.name))
            }
        }
        window.addEventListener("message", listener)
        onDispose { window.removeEventListener("message", listener) }
    }
}
