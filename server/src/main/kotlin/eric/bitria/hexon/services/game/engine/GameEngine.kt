package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.dtos.matchmaking.GameMessage

interface GameEngine {
    // Called once when the room hits maxPlayers
    suspend fun start(players: List<Players>, sender: GameMessageSender)

    // Called when a running game receives input
    suspend fun onMessage(userId: String, message: GameMessage)

    // Handle disconnects during the game
    suspend fun onPlayerLeave(userId: String)
    suspend fun onPlayerRejoin(userId: String)
}

data class Players(
    val userId: String,
    val username: String
)