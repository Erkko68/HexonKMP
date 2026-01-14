package eric.bitria.hexon.render

import kotlinx.browser.window
import org.w3c.dom.HTMLElement

// On Web, we just need a handle to satisfy the common API.
// We don't use 'element' for logic anymore.
actual class WebView(val element: HTMLElement) {
    var bridge: NativeWebBridge? = null
}

actual fun WebView.platformLoadDataWithBaseURL(
    baseUrl: String?,
    data: String,
    mimeType: String?,
    encoding: String?,
    historyUrl: String?,
) {
    // We intentionally disable this on Web to prevent accidental
    // overwriting of the entire Compose app (document.body).
    console.warn("WebView.loadData disabled on Web to protect the main window content.")
}

actual fun WebView.platformEvaluateJavascript(
    script: String,
    callback: ((String) -> Unit)?,
) {
    // DIRECT EXECUTION: Always run JS on the global window
    try {
        val result = window.asDynamic().eval(script)

        // Handle undefined/null results gracefully
        val strResult = if (result == null || result == undefined) "null" else result.toString()
        callback?.invoke(strResult)
    } catch (e: Throwable) {
        console.error("WebView Eval Error for script: $script", e)
        callback?.invoke("null")
    }
}

actual fun WebView.platformAddJavascriptInterface(
    obj: Any,
    name: String,
) {
    if (obj is NativeWebBridge) {
        this.bridge = obj

        val jsInterface = js("({})")
        jsInterface["call"] = { method: String, data: String?, callbackId: String? ->
            obj.call(method, data, callbackId)
        }

        window.asDynamic()[name] = jsInterface
    } else {
        window.asDynamic()[name] = obj
    }
}

@Target(AnnotationTarget.FUNCTION)
actual annotation class PlatformJavascriptInterface actual constructor()

actual abstract class PlatformContext