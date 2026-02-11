package eric.bitria.hexon.render

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import eric.bitria.hexon.config.EnvConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
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
                
                backgroundColor = platform.UIKit.UIColor.clearColor
                opaque = false
                
                // Disable scrolling
                scrollView.scrollEnabled = false
                scrollView.bounces = false
                
                // Fix safe area insets bug
                scrollView.contentInsetAdjustmentBehavior = platform.UIKit.UIScrollViewContentInsetAdjustmentBehavior.UIScrollViewContentInsetAdjustmentNever
            }
        }.also { state.webView = it }

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

    val currentContent = state.content
    LaunchedEffect(webView, currentContent, jsBridge) {
        webView.runOnMainThread {
            if (currentContent is WebContent.Data) {
                val html = if (currentContent.data.trim().startsWith("<")) {
                    currentContent.data
                } else {
                    wrapScriptInHtml(currentContent.data, jsBridge?.jsScript)
                }
                val baseUrl = platform.Foundation.NSURL(string = EnvConfig.BASE_URL)
                webView.loadHTMLString(html, baseURL = baseUrl)
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
                webView
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                webView.navigationDelegate = delegates
                webView.UIDelegate = delegates
            },
            onRelease = {
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
            $script
        </script>
    </body>
    </html>
""".trimIndent()
