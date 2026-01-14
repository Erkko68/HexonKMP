package eric.bitria.hexon.render

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
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


actual fun WebView.platformLoadDataWithBaseURL(
    baseUrl: String?,
    data: String,
    mimeType: String?,
    encoding: String?,
    historyUrl: String?,
) {
    loadHTMLString(data, baseURL = baseUrl?.let { NSURL.URLWithString(it) })
}

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
    configuration.userContentController.addScriptMessageHandler(
        scriptMessageHandler =
            object : NSObject(), WKScriptMessageHandlerProtocol {
                override fun userContentController(
                    userContentController: WKUserContentController,
                    didReceiveScriptMessage: WKScriptMessage,
                ) {
                    val body = didReceiveScriptMessage.body as? Map<String, Any?>
                    if (body != null && obj is NativeWebBridge) {
                        val method = body["method"] as? String
                        val data = body["data"] as? String
                        val callbackId = body["callbackId"] as? String
                        if (method != null) {
                            obj.call(method, data, callbackId)
                        }
                    }
                }
            },
        name = name,
    )

    if (obj is NativeWebBridge) {
        val adapterScript =
            """
            window.$name = {
                call: function(method, data, callbackId) {
                    var message = {
                        method: method,
                        data: data,
                        callbackId: callbackId
                    };
                    window.webkit.messageHandlers.$name.postMessage(message);
                }
            };
            """.trimIndent()

        val userScript =
            WKUserScript(
                source = adapterScript,
                injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
                forMainFrameOnly = false,
            )
        configuration.userContentController.addUserScript(userScript)
    }
}

@Target(AnnotationTarget.FUNCTION)
actual annotation class PlatformJavascriptInterface actual constructor()
