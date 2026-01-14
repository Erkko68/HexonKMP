@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package eric.bitria.hexon.render

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface

actual typealias WebView = android.webkit.WebView

actual fun WebView.platformLoadDataWithBaseURL(
    baseUrl: String?,
    data: String,
    mimeType: String?,
    encoding: String?,
    historyUrl: String?,
) = this.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)

actual fun WebView.platformEvaluateJavascript(
    script: String,
    callback: ((String) -> Unit)?,
) = this.evaluateJavascript(script, callback)

@SuppressLint("JavascriptInterface")
actual fun WebView.platformAddJavascriptInterface(
    obj: Any,
    name: String,
) = this.addJavascriptInterface(obj, name)

actual typealias PlatformJavascriptInterface = JavascriptInterface

actual typealias PlatformContext = Context
