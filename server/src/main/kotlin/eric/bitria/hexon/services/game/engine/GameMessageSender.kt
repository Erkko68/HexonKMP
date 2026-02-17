package eric.bitria.hexon.services.game.engine

import eric.bitria.hexon.game.data.PlayerId
import eric.bitria.hexon.ws.GameMessage


interface GameMessageSender {
    suspend fun sendToPlayer(receiverId: PlayerId, message: GameMessage)
    suspend fun broadcast(message: GameMessage)
    suspend fun broadcast(message: GameMessage, excludeUserId: PlayerId? = null)
}