package eric.bitria.hexon.services.matchmaking

import eric.bitria.hexon.dtos.matchmaking.JoinGameResponse

interface MatchmakingService {

    /**
     * Finds an existing session with available slots for the player,
     * or creates a new session if none are available
     */
    suspend fun findGameForPlayer(
        userId: String,
        mode: String = "classic",
        maxPlayers: Int = 4
    ): JoinGameResponse
}
