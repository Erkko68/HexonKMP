package eric.bitria.hexon.render

import kotlinx.cinterop.ExperimentalForeignApi
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.darwin.NSObject

fun <T> T.runOnMainThread(block: T.() -> Unit) {
    if (platform.Foundation.NSThread.isMainThread) {
        block()
    } else {
        platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
            block()
        }
    }
}

actual typealias WebView = WKWebView

actual abstract class PlatformContext

actual fun WebView.platformEvaluateJavascript(
    script: String,
    callback: ((String) -> Unit)?,
) {
    evaluateJavaScript(script) { result, error ->
        if (error == null && result != null) {
            callback?.invoke(result.toString())
        } else {
            callback?.invoke("null")
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun WebView.platformAddJavascriptInterface(
    obj: Any,
    name: String,
) {
    // 1. Cast early. If obj isn't the bridge, we can't use it, so return.
    val bridge = obj as? NativeWebBridge ?: return

    // 2. Register the Message Handler
    configuration.userContentController.addScriptMessageHandler(
        scriptMessageHandler = object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage,
            ) {
                // Flattened logic using safely casted variables
                val body = didReceiveScriptMessage.body as? Map<*, *>
                val method = body?.get("method") as? String ?: return // Guard: method is required

                bridge.call(
                    methodName = method,
                    data = body["data"] as? String,
                    callbackId = body["callbackId"] as? String
                )
            }
        },
        name = name,
    )

    // 3. Inject the Adapter Script
    // We inline the object creation inside postMessage for brevity
    val adapterScript = """
        window.$name = {
            call: function(method, data, callbackId) {
                window.webkit.messageHandlers.$name.postMessage({
                    method: method,
                    data: data,
                    callbackId: callbackId
                });
            }
        };
    """.trimIndent()

    configuration.userContentController.addUserScript(
        WKUserScript(
            source = adapterScript,
            injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
            forMainFrameOnly = false,
        )
    )
}

@Target(AnnotationTarget.FUNCTION)
actual annotation class PlatformJavascriptInterface actual constructor()
