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

// --- Private lobbies (create / join by 6-digit code / host-start) ---

// Create a private lobby; the caller becomes its host. The server mints the session
// and a short numeric [code] others use to join.
@Serializable
data class CreateLobbyRequest(val playerId: String)

@Serializable
data class CreateLobbyResponse(val gameId: String, val code: String)

// Join a private lobby by its [code]. Reserves the caller's seat (so the WS connect
// is authorized), mirroring how POST /game works. 404 if the code is unknown/full.
@Serializable
data class JoinLobbyRequest(val code: String, val playerId: String)

@Serializable
data class JoinLobbyResponse(val gameId: String)

// Host-only: start a private lobby. 409 if the caller isn't the host or the lobby
// isn't startable yet (below the minimum).
@Serializable
data class StartLobbyRequest(val gameId: String, val playerId: String)

@Serializable
data class ErrorResponse(val message: String)
