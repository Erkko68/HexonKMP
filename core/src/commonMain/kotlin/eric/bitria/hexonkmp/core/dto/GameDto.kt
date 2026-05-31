package eric.bitria.hexonkmp.core.dto

import kotlinx.serialization.Serializable

@Serializable
data class JoinGameResponse(
    val playerId: String,
    val gameId: String,
)

@Serializable
data class ErrorResponse(val message: String)
