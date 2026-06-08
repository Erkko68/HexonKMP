package eric.bitria.hexonkmp.core.protocol

import kotlinx.serialization.Serializable

// Identity handshake, separate from matchmaking. The client sends its chosen [name]
// and, if it has one from a previous session, its secret [token] to prove identity.
// The server is authoritative: it returns the [playerId] the token maps to (minting a
// fresh playerId + token when no valid token is supplied). Every later request
// presents the token instead of asserting a playerId, so the server — not the client
// — decides who the caller is. The token is stored only on the device.
@Serializable
data class RegisterRequest(val name: String, val token: String? = null)

@Serializable
data class RegisterResponse(val playerId: String, val name: String, val token: String)

// Matchmaking / lobby requests authenticate with the bearer token in the
// Authorization header (handled centrally by the client's Auth plugin); the server
// resolves the playerId from it. A client never names another player. /game and
// /lobby need no body at all — identity is the header.

@Serializable
data class JoinGameResponse(val gameId: String)

// --- Private lobbies (create / join by 6-digit code / host-start) ---

@Serializable
data class CreateLobbyResponse(val gameId: String, val code: String)

// Join a private lobby by its [code]. 404 if the code is unknown/full.
@Serializable
data class JoinLobbyRequest(val code: String)

@Serializable
data class JoinLobbyResponse(val gameId: String)

// Host-only: start a private lobby. 409 if the caller isn't the host or the lobby
// isn't startable yet (below the minimum).
@Serializable
data class StartLobbyRequest(val gameId: String)

@Serializable
data class ErrorResponse(val message: String)
