@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package eric.bitria.hexon.render

expect class WebView

/**
 * Loads the given data with the base URL.
 */
expect fun WebView.platformLoadDataWithBaseURL(
    baseUrl: String?,
    data: String,
    mimeType: String?,
    encoding: String?,
    historyUrl: String?,
)

/**
 * Evaluates the given JavaScript.
 */
expect fun WebView.platformEvaluateJavascript(
    script: String,
    callback: ((String) -> Unit)?,
)

expect fun WebView.platformAddJavascriptInterface(
    obj: Any,
    name: String,
)

@Target(AnnotationTarget.FUNCTION)
expect annotation class PlatformJavascriptInterface()

expect abstract class PlatformContext
