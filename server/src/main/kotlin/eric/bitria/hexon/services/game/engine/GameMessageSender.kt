package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.dtos.matchmaking.GameMessage

interface GameMessageSender {
    suspend fun sendToPlayer(userId: String, message: GameMessage)
    suspend fun broadcast(message: GameMessage)
}