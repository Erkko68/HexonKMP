package eric.bitria.hexon.services.game

import eric.bitria.hexon.ws.GameMessage
import io.ktor.websocket.DefaultWebSocketSession

interface GameSession {
    val sessionId: String
    val isGameStarted: Boolean

    // Matchmaking Logic
    suspend fun reserveSlot(userId: String): Boolean
    fun hasAvailableSlots(): Boolean

    // WebSocket Logic
    suspend fun connectPlayer(userId: String, username: String, session: DefaultWebSocketSession): Boolean
    suspend fun removePlayer(userId: String)
    suspend fun handleIncomingMessage(userId: String, message: GameMessage)
}