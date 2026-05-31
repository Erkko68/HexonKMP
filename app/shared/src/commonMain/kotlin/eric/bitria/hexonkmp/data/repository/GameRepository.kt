package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.core.dto.JoinGameResponse
import eric.bitria.hexonkmp.core.ws.ServerEvent
import kotlinx.coroutines.flow.Flow

interface GameRepository {
    suspend fun joinGame(): JoinGameResponse
    fun connect(playerId: String, gameId: String)
    fun disconnect()
    val events: Flow<ServerEvent>
}
