package eric.bitria.hexon.render

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.view.ViewGroup.LayoutParams
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal actual fun ComposeWebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    jsBridge: WebViewJsBridge?,
) {
    val webView = state.webView
    val lifecycleOwner = LocalLifecycleOwner.current

    webView?.let { wv ->
        LaunchedEffect(wv, state, jsBridge) {
            snapshotFlow { state.content }.collect { content ->
                if (content is WebContent.Data) {
                    val html = if (content.data.trim().startsWith("<")) {
                        content.data
                    } else {
                        wrapScriptInHtml(content.data, jsBridge?.jsScript)
                    }
                    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                }
            }
        }
    }

    WebViewContainer(modifier = modifier) { layoutParams ->
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    this.layoutParams = layoutParams
                    
                    setBackgroundColor(Color.TRANSPARENT)
                    
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            state.loadingState = LoadingState.Loading(0f)
                        }
                        override fun onPageFinished(view: WebView?, url: String?) {
                            state.loadingState = LoadingState.Finished
                        }
                    }
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            state.loadingState = if (newProgress == 100) LoadingState.Finished else LoadingState.Loading(newProgress / 100f)
                        }
                    }

                    jsBridge?.attach(this)
                }.also { wv ->
                    state.webView = wv
                }
            },
            modifier = Modifier,
            update = { _ -> },
            onRelease = {
                state.webView = null
                it.destroy()
            },
        )
    }

    DisposableEffect(lifecycleOwner, webView) {
        val observer = LifecycleEventObserver { _, event ->
            webView?.let { wv ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> { wv.onResume(); wv.resumeTimers() }
                    Lifecycle.Event.ON_PAUSE -> { wv.onPause(); wv.pauseTimers() }
                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun WebViewContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.(FrameLayout.LayoutParams) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val width = if (constraints.hasFixedWidth) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
        val height = if (constraints.hasFixedHeight) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
        content(FrameLayout.LayoutParams(width, height))
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
