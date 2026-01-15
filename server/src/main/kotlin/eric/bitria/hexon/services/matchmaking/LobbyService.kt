package eric.bitria.hexon.services.matchmaking

import eric.bitria.hexon.dtos.matchmaking.CreateLobbyResponse

interface LobbyService {

    /** Create a custom lobby */
    suspend fun createCustomGame(
        creatorId: String,
        mode: String,
        maxPlayers: Int,
    ): CreateLobbyResponse

    /** Invite a player to a custom game */
    suspend fun invitePlayer(sessionId: String, invitedUserId: String): Boolean
}