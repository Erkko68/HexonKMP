package eric.bitria.hexonkmp.core.protocol

import kotlinx.serialization.Serializable

@Serializable
data class JoinGameRequest(val playerId: String)

@Serializable
data class JoinGameResponse(
    val playerId: String,
    val gameId: String,
)

@Serializable
data class ErrorResponse(val message: String)
