package eric.bitria.hexon.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    controller: WebViewController,
    javaScriptInterfaces: Map<String, Any>,
    onCreated: (eric.bitria.hexon.render.WebView) -> Unit,
    onDispose: (eric.bitria.hexon.render.WebView) -> Unit,
    onConsoleMessage: (ConsoleMessage) -> Unit,
    jsBridge: WebViewJsBridge?,
) {
    val webView =
        state.webView ?: remember {
            val config = WKWebViewConfiguration().apply {
                allowsInlineMediaPlayback = true
                mediaTypesRequiringUserActionForPlayback = platform.WebKit.WKAudiovisualMediaTypeNone
            }
            WKWebView(frame = kotlinx.cinterop.cValue { }, configuration = config).apply {
                allowsBackForwardNavigationGestures = false
                autoresizingMask = platform.UIKit.UIViewAutoresizingFlexibleWidth or platform.UIKit.UIViewAutoresizingFlexibleHeight
                
                // Disable scrolling
                scrollView.scrollEnabled = false
                scrollView.bounces = false
                
                // Fix safe area insets bug
                scrollView.contentInsetAdjustmentBehavior = platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
            }
        }.also { state.webView = it }

    val registeredInterfaceNames = remember(webView) { mutableSetOf<String>() }

    val delegates = remember {
        object : NSObject(), WKNavigationDelegateProtocol, WKUIDelegateProtocol {
            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didStartProvisionalNavigation: platform.WebKit.WKNavigation?) {
                state.loadingState = LoadingState.Loading(0f)
            }

            @ObjCSignatureOverride
            override fun webView(webView: WKWebView, didFinishNavigation: platform.WebKit.WKNavigation?) {
                state.loadingState = LoadingState.Finished
            }

            override fun webView(webView: WKWebView, didFailNavigation: platform.WebKit.WKNavigation?, withError: platform.Foundation.NSError) {
                state.loadingState = LoadingState.Failed(WebViewError(errorCode = withError.code.toInt(), description = withError.localizedDescription))
            }
        }
    }

    // Console message handler
    DisposableEffect(webView) {
        val messageHandler = object : NSObject(), WKScriptMessageHandlerProtocol {
            override fun userContentController(
                userContentController: WKUserContentController,
                didReceiveScriptMessage: WKScriptMessage
            ) {
                val body = didReceiveScriptMessage.body as? Map<String, Any?>
                if (body != null) {
                    val message = body["message"] as? String ?: ""
                    val levelStr = body["level"] as? String ?: "LOG"
                    val level = when (levelStr) {
                        "LOG" -> ConsoleMessageLevel.LOG
                        "DEBUG" -> ConsoleMessageLevel.DEBUG
                        "WARN" -> ConsoleMessageLevel.WARNING
                        "ERROR" -> ConsoleMessageLevel.ERROR
                        else -> ConsoleMessageLevel.LOG
                    }
                    onConsoleMessage(ConsoleMessage(message = message, level = level))
                }
            }
        }
        
        val consoleScript = """
            (function() {
                var oldLog = console.log;
                var oldWarn = console.warn;
                var oldError = console.error;
                var oldDebug = console.debug;

                function sendToNative(level, args) {
                    var message = Array.from(args).map(arg => {
                        if (typeof arg === 'object') return JSON.stringify(arg);
                        return String(arg);
                    }).join(' ');
                    window.webkit.messageHandlers.console.postMessage({
                        level: level,
                        message: message
                    });
                }

                console.log = function() {
                    sendToNative('LOG', arguments);
                    oldLog.apply(console, arguments);
                };
                console.warn = function() {
                    sendToNative('WARN', arguments);
                    oldWarn.apply(console, arguments);
                };
                console.error = function() {
                    sendToNative('ERROR', arguments);
                    oldError.apply(console, arguments);
                };
                console.debug = function() {
                    sendToNative('DEBUG', arguments);
                    oldDebug.apply(console, arguments);
                };
            })();
        """.trimIndent()

        webView.configuration.userContentController.addScriptMessageHandler(messageHandler, "console")
        val userScript = WKUserScript(
            source = consoleScript,
            injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
            forMainFrameOnly = false
        )
        webView.configuration.userContentController.addUserScript(userScript)

        onDispose {
            webView.configuration.userContentController.removeScriptMessageHandlerForName("console")
        }
    }

    LaunchedEffect(webView, controller) {
        controller.handleNavigationEvents(webView)
    }

    val currentContent = state.content
    LaunchedEffect(webView, currentContent, jsBridge) {
        webView.runOnMainThread {
            if (currentContent is WebContent.Data) {
                val html = if (currentContent.data.trim().startsWith("<")) {
                    currentContent.data
                } else {
                    wrapScriptInHtml(currentContent.data, jsBridge?.jsScript)
                }
                webView.loadHTMLString(html, baseURL = null)
            }
        }
    }

    LaunchedEffect(webView, javaScriptInterfaces) {
        webView.runOnMainThread {
            val userController = webView.configuration.userContentController
            registeredInterfaceNames.forEach { userController.removeScriptMessageHandlerForName(it) }
            registeredInterfaceNames.clear()
            javaScriptInterfaces.forEach { (name, obj) ->
                webView.platformAddJavascriptInterface(obj, name)
                registeredInterfaceNames.add(name)
            }
        }
    }

    DisposableEffect(webView) {
        onDispose {
            webView.runOnMainThread {
                val userController = webView.configuration.userContentController
                registeredInterfaceNames.forEach { userController.removeScriptMessageHandlerForName(it) }
                registeredInterfaceNames.clear()
            }
        }
    }

    LaunchedEffect(webView, jsBridge) {
        jsBridge?.attach(webView)
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        UIKitView(
            factory = {
                webView.navigationDelegate = delegates
                webView.UIDelegate = delegates
                onCreated(webView)
                webView
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                webView.navigationDelegate = delegates
                webView.UIDelegate = delegates
            },
            onRelease = {
                onDispose(webView)
                if (state.webView === webView) {
                    state.webView = null
                }
            },
        )
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
                background: black;
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
