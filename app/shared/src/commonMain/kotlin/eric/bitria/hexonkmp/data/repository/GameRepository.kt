package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.core.game.action.GameAction
import eric.bitria.hexonkmp.core.protocol.CatanServerEvent
import eric.bitria.hexonkmp.core.protocol.JoinGameResponse
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    val events: Flow<CatanServerEvent>
    suspend fun joinGame(playerId: String): JoinGameResponse
    fun connect(playerId: String, gameId: String)
    fun sendAction(action: GameAction)
    fun disconnect()
}
