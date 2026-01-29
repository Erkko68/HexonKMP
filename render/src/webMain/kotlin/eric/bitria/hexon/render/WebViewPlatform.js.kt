package eric.bitria.hexon.render

import org.w3c.dom.HTMLIFrameElement


actual class WebView {
    // Change to nullable var
    var iframe: HTMLIFrameElement? = null
    var bridge: NativeWebBridge? = null
}

actual fun WebView.platformEvaluateJavascript(
    script: String,
    callback: ((String) -> Unit)?,
) {
    val contentWindow = iframe?.contentWindow ?: run {
        callback?.invoke("null")
        return
    }

    val safeEval = js("""
        (function(win, s) {
            try {
                return win.eval(s);
            } catch (e) {
                console.error("WebView JS Eval Error:", e);
                return null;
            }
        })
    """)

    val result = safeEval(contentWindow, script)
    callback?.invoke(result?.toString() ?: "null")
}

actual fun WebView.platformAddJavascriptInterface(
    obj: Any,
    name: String,
) {
    val win = iframe?.contentWindow ?: return

    if (obj is NativeWebBridge) {
        this.bridge = obj

        val jsInterface = js("({})")
        jsInterface["call"] = { method: String, data: String?, callbackId: String? ->
            obj.call(method, data, callbackId)
        }

        win.asDynamic()[name] = jsInterface
    } else {
        win.asDynamic()[name] = obj
    }
}


@Target(AnnotationTarget.FUNCTION)
actual annotation class PlatformJavascriptInterface actual constructor()

actual abstract class PlatformContext
