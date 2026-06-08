package eric.bitria.hexonkmp.core.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Server → client envelope, generic over a game's state [S] and event [E] types
// so the transport can carry ANY game. Declaration-site variance lets the
// game-agnostic lifecycle messages be ServerEvent<Nothing, Nothing> — they carry
// no game payload, so they need no phantom type parameters. Only GameStarted
// (snapshot) and GameUpdate (one domain event) are game-specific.
@Serializable
sealed interface ServerEvent<out S, out E>

// --- Lobby / matchmaking phase ---

// [countdownSeconds] is the auto-start delay remaining: non-null once the lobby has
// reached the minimum and the countdown is running (the client ticks it down locally
// — the server sends it only on lobby changes, not every second), null otherwise.
@Serializable
@SerialName("WaitingForPlayers")
data class WaitingForPlayers(
    val connected: Int,
    val needed: Int,
    val countdownSeconds: Int? = null,
) : ServerEvent<Nothing, Nothing>

// Carries the initial snapshot when the room fills, and the current snapshot
// when a player reconnects into an already-running game.
@Serializable
@SerialName("GameStarted")
data class GameStarted<out S>(val state: S) : ServerEvent<S, Nothing>

// --- Presence (in-game) ---

@Serializable
@SerialName("PlayerJoined")
data class PlayerJoined(val playerId: String) : ServerEvent<Nothing, Nothing>

@Serializable
@SerialName("PlayerLeft")
data class PlayerLeft(val playerId: String) : ServerEvent<Nothing, Nothing>

// --- Game updates ---

// Wraps a domain event produced by the engine's reduce().
@Serializable
@SerialName("GameUpdate")
data class GameUpdate<out E>(val event: E) : ServerEvent<Nothing, E>

// Sent only to the acting player when their action failed validation.
@Serializable
@SerialName("ActionRejected")
data class ActionRejected(val reason: String) : ServerEvent<Nothing, Nothing>

// --- Client-local only ---

// Never sent over the wire — emitted by the client when the WebSocket fails or
// drops, so the UI can leave its loading state.
@Serializable
@SerialName("ConnectionFailed")
data class ConnectionFailed(val reason: String) : ServerEvent<Nothing, Nothing>
