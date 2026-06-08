package eric.bitria.hexonkmp.core.protocol

import kotlinx.serialization.Serializable

// Identity handshake, separate from matchmaking. The client sends its chosen
// [name] and, if it has one from a previous session, its [existingPlayerId] so the
// server can reuse the same identity (reconnection). The server is authoritative:
// it mints the [playerId] when none is supplied (or the supplied one is unknown).
// This is the seam a real auth flow (credentials -> token -> identity) replaces.
@Serializable
data class RegisterRequest(val name: String, val existingPlayerId: String? = null)

@Serializable
data class RegisterResponse(val playerId: String, val name: String)

@Serializable
data class JoinGameRequest(val playerId: String)

@Serializable
data class JoinGameResponse(
    val playerId: String,
    val gameId: String,
)

@Serializable
data class ErrorResponse(val message: String)
