package eric.bitria.hexon.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Interface for game screen rendering and communication.
 * Provides a platform-agnostic way to render game content and exchange JSON messages.
 */
interface GameRender {
    /**
     * Renders the game screen content.
     * @param modifier Compose modifier for layout customization
     */
    @Composable
    fun Render(modifier: Modifier = Modifier)

    /**
     * Sends JSON data to the game screen.
     * @param json The JSON string to send to the game
     * @throws Exception if the game screen isn't ready or communication fails
     */
    suspend fun sendJson(json: String)

    /**
     * Registers a callback to receive JSON data from the game screen.
     * @param callback Suspending function that receives JSON strings from the game
     */
    fun receiveJson(callback: suspend (String) -> Unit)
}