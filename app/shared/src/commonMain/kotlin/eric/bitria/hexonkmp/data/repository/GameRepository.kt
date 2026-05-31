package eric.bitria.hexonkmp.data.repository

import eric.bitria.hexonkmp.core.dto.JoinGameResponse

interface GameRepository {
    suspend fun joinGame(): JoinGameResponse
}
