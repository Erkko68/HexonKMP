package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.client.GameClient
import eric.bitria.hexonkmp.core.dto.JoinGameResponse

class GameRepositoryImpl(private val client: GameClient) : GameRepository {
    override suspend fun joinGame(): JoinGameResponse = client.joinGame()
}
