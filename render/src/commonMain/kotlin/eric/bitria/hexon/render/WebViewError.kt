package eric.bitria.hexon.render

import androidx.compose.runtime.Immutable

/**
 * Represents an error that occurred in the WebView.
 */
@Immutable
data class WebViewError(
    val errorCode: Int = 0,
    val description: String = "Unknown error"
)
