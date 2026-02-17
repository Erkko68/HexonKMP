package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.ws.GameMessage
import eric.bitria.hexon.ws.lobby.LobbyPlayer


interface GameEngine {
    // Called once when the room hits maxPlayers
    suspend fun start(gameId: String, lobbyPlayers: List<LobbyPlayer>, sender: GameMessageSender)

    // Called to end the game (broadcasts GameEnded and returns control to session)
    suspend fun endGame(winnerId: String?)

    // Called when a running game receives input
    suspend fun onMessage(userId: String, message: GameMessage)

    // Handle disconnects during the game
    suspend fun onPlayerLeave(userId: String)
    suspend fun onPlayerRejoin(userId: String)
}