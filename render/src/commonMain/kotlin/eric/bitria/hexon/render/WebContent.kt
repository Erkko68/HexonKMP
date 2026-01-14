package eric.bitria.hexon.render

/**
 * Sealed interface representing the content to be loaded into the WebView.
 * Simplified to only support raw HTML data.
 */
sealed interface WebContent {
    /**
     * Represents raw data to be loaded into the WebView.
     *
     * @property data The data to load (e.g., HTML string).
     */
    data class Data(val data: String) : WebContent
}
