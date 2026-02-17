package eric.bitria.hexon.services.matchmaking

import eric.bitria.hexon.dtos.matchmaking.JoinGameResponse
import eric.bitria.hexon.dtos.matchmaking.JoinGameResult
import eric.bitria.hexon.services.game.GameSessionRepository
import eric.bitria.hexon.services.game.session.MatchmakingGameSession
import eric.bitria.hexon.ws.lobby.GameMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MatchmakingServiceImpl(
    private val repository: GameSessionRepository
) : MatchmakingService {

    private val mutex = Mutex()

    override suspend fun findGameForPlayer(
        userId: String,
        mode: GameMode,
        maxPlayers: Int
    ): JoinGameResponse = mutex.withLock {
        try {
            // Only Allow Classic for now
            if (mode != GameMode.CLASSIC) {
                return JoinGameResponse(
                    status = JoinGameResult.INVALID_MODE,
                    message = "Invalid game mode"
                )
            }
            // 1. Try to find an existing session with available slots using the optimized repository method
            val availableSession = repository.findAvailableSession(mode)

            if (availableSession != null) {
                val reserved = availableSession.reserveSlot(userId)
                if (reserved) {
                    return JoinGameResponse(
                        status = JoinGameResult.SUCCESS,
                        message = "Joined existing session",
                        sessionId = availableSession.sessionId
                    )
                }
            }

            // 2. No session found or reservation failed, create a new one
            val newSession = MatchmakingGameSession(
                mode = mode,
                maxPlayers = maxPlayers
            )
            
            val reserved = newSession.reserveSlot(userId)
            if (reserved) {
                repository.addSession(mode, newSession)
                return JoinGameResponse(
                    status = JoinGameResult.SUCCESS,
                    message = "Created new session",
                    sessionId = newSession.sessionId
                )
            }

            return JoinGameResponse(
                status = JoinGameResult.SESSION_FULL,
                message = "Failed to reserve slot for session."
            )
        } catch (e: Exception) {
            JoinGameResponse(
                status = JoinGameResult.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error occurred"
            )
        }
    }
}
