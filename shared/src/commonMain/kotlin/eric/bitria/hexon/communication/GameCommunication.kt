package eric.bitria.hexon.communication

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
/**
 * Handles communication between the native app and game screen.
 * Contains all business logic for JSON communication without UI concerns.
 */
class GameCommunication {
    private val _gameEvents = MutableSharedFlow<String>()
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    private var sendJsonHandler: (suspend (String) -> Unit)? = null

    /**
     * Sends JSON data to the game screen.
     * @param json The JSON string to send to the game
     * @throws Exception if the game screen isn't ready or communication fails
     */
    suspend fun sendJson(json: String) {
        sendJsonHandler?.invoke(json) ?: throw IllegalStateException("Game screen not ready")
    }

    /**
     * Sets the platform-specific JSON sending implementation.
     * Called by UI implementations to provide the actual sending mechanism.
     */
    fun setSendJsonHandler(handler: suspend (String) -> Unit) {
        sendJsonHandler = handler
    }

    /**
     * Called by platform implementations when JSON is received from the game.
     * @param json The JSON string received from the game
     */
    suspend fun handleReceivedJson(json: String) {
        _gameEvents.emit(json)
    }
}