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

            val scriptData = content.data

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
