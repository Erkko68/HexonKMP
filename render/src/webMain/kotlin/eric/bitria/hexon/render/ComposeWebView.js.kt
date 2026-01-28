package eric.bitria.hexon.render

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLScriptElement

@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    jsBridge: WebViewJsBridge?,
) {
    val webView = remember {
        eric.bitria.hexon.render.WebView(document.createElement("div") as HTMLElement)
    }

    val contentInjectionId = "content-injection-point"
    val bridgeInjectionId = "bridge-injection-point"
    val canvasId = "three-root"

    DisposableEffect(Unit) {
        state.webView = webView

        onDispose {
            state.webView = null
            document.getElementById(contentInjectionId)?.remove()
            document.getElementById(bridgeInjectionId)?.remove()
            document.getElementById(canvasId)?.remove()
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
}
