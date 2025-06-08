package eric.bitria.hexon

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import hexonkmp.composeapp.generated.resources.Res
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {

    var html by remember { mutableStateOf("") }
    var js by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        html = Res.readBytes("files/index.html").decodeToString()
        js = Res.readBytes("files/bundle.js").decodeToString()
    }

    MaterialTheme {
        ThreeJsWebView(html.replace("/*CODE*/", js))
    }
}

@Composable
fun ThreeJsWebView(html: String) {
    val webViewState = rememberWebViewStateWithHTMLData(
        data = html
    )
    WebView(webViewState, modifier = Modifier.fillMaxSize())
}