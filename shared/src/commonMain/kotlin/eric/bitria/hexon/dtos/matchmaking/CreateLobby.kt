package eric.bitria.hexon.dtos.matchmaking

import eric.bitria.hexon.ws.lobby.GameMode
import kotlinx.serialization.Serializable

@Serializable
data class CreateLobbyRequest(
    val mode: GameMode,
    val maxPlayers: Int,
)

@Serializable
data class CreateLobbyResponse(
    val status: CreateLobbyResult,
    val message: String,
    val sessionId: String? = null,
)

@Serializable
enum class CreateLobbyResult {
    SUCCESS,
    INVALID_MODE,
    INVALID_MAX_PLAYERS,
    UNKNOWN_ERROR
}
