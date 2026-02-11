package eric.bitria.hexon.data.repository

import eric.bitria.hexon.data.remote.MatchmakingClient
import eric.bitria.hexon.dtos.matchmaking.JoinGameRequest
import eric.bitria.hexon.dtos.matchmaking.JoinGameResponse
import eric.bitria.hexon.ws.lobby.GameMode

interface MatchmakingRepository {
    suspend fun joinGame(mode: GameMode): ApiResult<JoinGameResponse>
}

class MatchmakingRepositoryImpl(
    private val client: MatchmakingClient
) : MatchmakingRepository {

    override suspend fun joinGame(mode: GameMode): ApiResult<JoinGameResponse> {
        return safeApiCall {
            client.joinGame(JoinGameRequest(mode = mode))
        }
    }
}
