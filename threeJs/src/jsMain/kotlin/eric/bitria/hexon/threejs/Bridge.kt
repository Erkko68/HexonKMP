package eric.bitria.hexon.threejs

import kotlin.js.Promise

/**
 * External declaration for the object injected by WebViewJsBridge.kt
 * Matches the structure: window.AppBridge = { call: ..., on: ... }
 */
external object AppBridge {
    /**
     * Calls a method on the Native side.
     * @param method The name of the handler registered in Kotlin.
     * @param data The data to send (optional).
     * @return A Promise that resolves with the result from the native target.
     */
    fun call(method: String, data: Any? = definedExternally): Promise<dynamic>

    /**
     * Registers a listener for events sent from the web view.
     * @param event The event name (e.g., "updateSpeed").
     * @param callback The function to run when the event is triggered.
     */
    fun on(event: String, callback: (dynamic) -> Unit)
}