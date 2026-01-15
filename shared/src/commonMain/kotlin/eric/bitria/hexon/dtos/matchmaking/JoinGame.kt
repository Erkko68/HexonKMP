package eric.bitria.hexon.dtos.matchmaking

import kotlinx.serialization.Serializable

@Serializable
data class JoinGameRequest(
    val mode: String
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
    UNKNOWN_ERROR
}
