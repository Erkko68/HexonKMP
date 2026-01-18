package eric.bitria.hexon.dtos.matchmaking

import eric.bitria.hexon.ws.data.GameMode
import kotlinx.serialization.Serializable

@Serializable
data class JoinGameRequest(
    val mode: GameMode
)

@Serializable
data class JoinGameResponse(
    val status: JoinGameResult,
    val message: String,
    val sessionId: String? = null,
)

@Serializable
enum class JoinGameResult {
    SUCCESS,
    INVALID_MODE,
    SESSION_FULL,
    UNKNOWN_ERROR
}
