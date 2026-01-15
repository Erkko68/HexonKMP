package eric.bitria.hexon.services.game

import eric.bitria.hexon.services.game.engine.GameEngine
import io.ktor.websocket.DefaultWebSocketSession

interface GameSession {

    val sessionId: String
    val mode: String
    val maxPlayers: Int

    /** Returns all connected players */
    fun connectedPlayers(): Set<String>

    /** Returns reserved (not yet connected) players */
    fun reservedPlayers(): Set<String>

    /** Reserve a slot for a player (quick join / invite). Returns false if full */
    suspend fun reserveSlot(userId: String, timeoutMs: Long = 5000L): Boolean

    /** Player connects via WebSocket. Returns false if slot not reserved */
    suspend fun connectPlayer(userId: String, ws: DefaultWebSocketSession): Boolean

    /** Remove player (disconnect or leave) */
    suspend fun removePlayer(userId: String)

    /** Check if all required players are ready */
    fun isReady(): Boolean

    /** Cleanup expired reserved slots */
    suspend fun cleanupExpiredSlots(timeoutMs: Long = 5000L)

    /** Invite a specific player for custom lobby (reserve a slot) */
    suspend fun invitePlayer(userId: String): Boolean

    /** Start the game once all players are ready. Returns the GameEngine */
    fun startGame(): GameEngine?
}
