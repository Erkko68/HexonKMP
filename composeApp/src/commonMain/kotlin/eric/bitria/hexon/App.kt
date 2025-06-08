package eric.bitria.hexon

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.WebViewNavigator
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import hexonkmp.composeapp.generated.resources.Res

@Composable
fun App() {
    var html by remember { mutableStateOf("") }
    var js by remember { mutableStateOf("") }
    // Persist both values across configuration changes
    var scale by rememberSaveable { mutableStateOf(1f) }
    var quaternion by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        html = Res.readBytes("files/index.html").decodeToString()
        js = Res.readBytes("files/bundle.js").decodeToString()
    }

    MaterialTheme {
        ThreeJsWebView(
            html = html.replace("/*CODE*/", js),
            scale = scale,
            onScaleChange = { scale = it },
            quaternion = quaternion,
            onQuaternionChange = { quaternion = it }
        )
    }
}

@Composable
fun ThreeJsWebView(
    html: String,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    quaternion: String,
    onQuaternionChange: (String) -> Unit
) {
    val webViewState = rememberWebViewStateWithHTMLData(data = html)
    val navigator = rememberWebViewNavigator()
    val bridge = rememberWebViewJsBridge()
    val loadingState = webViewState.loadingState

    // Register bridge handler once
    LaunchedEffect(bridge) {
        bridge.register(QuaternionMessageHandler { onQuaternionChange(it) })
    }

    // 1. Restore state when page finishes loading
    LaunchedEffect(loadingState) {
        if (loadingState is LoadingState.Finished) {
            // Apply saved scale
            navigator.evaluateJavaScript("cube.scale.set($scale, $scale, $scale);")

            // Apply saved rotation if exists
            if (quaternion.isNotEmpty()) {
                navigator.evaluateJavaScript("cube.quaternion.set($quaternion);")
            }
        }
    }

    // 2. Handle scale changes (separate from initial restore)
    LaunchedEffect(scale) {
        // Only execute if page is loaded
        if (loadingState is LoadingState.Finished) {
            navigator.evaluateJavaScript("cube.scale.set($scale, $scale, $scale);")
        }
    }

    Column {
        Text(
            "Three.js in Compose Multiplatform!",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.headlineSmall
        )
        WebView(
            state = webViewState,
            modifier = Modifier.weight(1f),
            navigator = navigator,
            webViewJsBridge = bridge
        )
        Slider(
            value = scale,
            onValueChange = onScaleChange,
            modifier = Modifier.padding(12.dp),
            valueRange = 0.1f..2f
        )
        Text(
            quaternion,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

class QuaternionMessageHandler(val handler: (String) -> Unit) : IJsMessageHandler {
    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit
    ) {
        handler(message.params)
    }

    override fun methodName(): String = "Quaternion"
}
