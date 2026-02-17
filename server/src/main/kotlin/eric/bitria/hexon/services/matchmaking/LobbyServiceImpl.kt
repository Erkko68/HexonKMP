package eric.bitria.hexon.services.matchmaking

import eric.bitria.hexon.dtos.matchmaking.CreateLobbyResponse
import eric.bitria.hexon.dtos.matchmaking.CreateLobbyResult
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.game.session.CustomLobbySession
import eric.bitria.hexon.ws.lobby.GameMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LobbyServiceImpl(
    private val repository: GameSessionRepository
) : LobbyService {

    private val mutex = Mutex()

    override suspend fun createCustomGame(
        creatorId: String,
        mode: GameMode,
        maxPlayers: Int
    ): CreateLobbyResponse = mutex.withLock {
        try {
            // Validate mode
            if (mode != GameMode.CLASSIC) {
                return CreateLobbyResponse(
                    status = CreateLobbyResult.INVALID_MODE,
                    message = "Invalid game mode"
                )
            }

            // Validate max players
            if (maxPlayers !in 2..6) {
                return CreateLobbyResponse(
                    status = CreateLobbyResult.INVALID_MAX_PLAYERS,
                    message = "Max players must be between 2 and 6"
                )
            }

            // Create custom lobby session
            val lobbySession = CustomLobbySession(
                mode = mode,
                maxPlayers = maxPlayers
            )

            // Reserve slot for creator
            val reserved = lobbySession.reserveSlot(creatorId)
            if (!reserved) {
                return CreateLobbyResponse(
                    status = CreateLobbyResult.UNKNOWN_ERROR,
                    message = "Failed to reserve slot for creator"
                )
            }

            // Add to repository
            repository.addSession(mode, lobbySession)

            return CreateLobbyResponse(
                status = CreateLobbyResult.SUCCESS,
                message = "Custom lobby created",
                sessionId = lobbySession.sessionId
            )
        } catch (e: Exception) {
            return CreateLobbyResponse(
                status = CreateLobbyResult.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error occurred"
            )
        }
    }

    override suspend fun invitePlayer(sessionId: String, invitedUserId: String): Boolean {
        // TODO: Implement invite system when notification infrastructure is ready
        // This will require:
        // - Notification service to send invite to user
        // - Invite tracking (pending invites, expiration)
        // - Accept/decline handling
        return false
    }
}

