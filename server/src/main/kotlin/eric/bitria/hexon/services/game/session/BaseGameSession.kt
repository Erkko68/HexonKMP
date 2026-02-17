package eric.bitria.hexon.services.game.session

import eric.bitria.hexon.ws.GameMessage
import io.ktor.websocket.DefaultWebSocketSession

/**
 * Base interface for all game session types (matchmaking and custom lobbies).
 * Handles player connections, slot reservations, and message routing.
 */
interface BaseGameSession {

    /** Persistent session identifier (lobby ID) */
    val sessionId: String

    /** Check if there are available player slots */
    fun hasAvailableSlots(): Boolean

    /**
     * Reserve a slot for a player before they connect via WebSocket.
     * @return true if slot was reserved, false if session is full
     */
    suspend fun reserveSlot(userId: String): Boolean

    /**
     * Connect a player's WebSocket to this session.
     * @return true if connection was accepted, false otherwise
     */
    suspend fun connectPlayer(userId: String, username: String, session: DefaultWebSocketSession): Boolean

    /**
     * Remove a player from the session (disconnect or leave).
     */
    suspend fun removePlayer(userId: String)

    /**
     * Handle an incoming message from a connected player.
     */
    suspend fun handleIncomingMessage(userId: String, message: GameMessage)

    /**
     * Set the lifecycle listener for this session.
     * Should be called by the repository when adding the session.
     */
    fun setLifecycleListener(listener: SessionLifecycleListener)
}

