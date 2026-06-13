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

// A snapshot of who's in a pre-game lobby, pushed on every lobby change (a join,
// a leave, or a host handoff). One representation serves both lobby policies:
//  - auto / matchmaking: [hostId] is null and [countdownSeconds] counts down to an
//    automatic start (the client ticks it locally; the server sends it only on
//    changes, not every second).
//  - manual / private:  [hostId] names the host who must press Start; no countdown.
@Serializable
@SerialName("LobbyRoster")
data class LobbyRoster(
    val members: List<LobbyMember>,
    val hostId: String? = null,
    val minPlayers: Int,
    val maxPlayers: Int,
    val countdownSeconds: Int? = null,
) : ServerEvent<Nothing, Nothing>

// One connected player in a lobby roster: their id and chosen display name.
@Serializable
data class LobbyMember(val id: String, val name: String)

// Carries the initial snapshot when the room fills, and the current snapshot
// when a player reconnects into an already-running game. [playerNames] is the
// public roster (playerId -> display name) carried as transport metadata — names
// are display-only and never enter the pure game state.
@Serializable
@SerialName("GameStarted")
data class GameStarted<out S>(
    val state: S,
    val playerNames: Map<String, String> = emptyMap(),
) : ServerEvent<S, Nothing>

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

// The current player's turn clock. Broadcast whenever the active turn changes (or
// the timer re-arms): [remainingSeconds] is how long they have left right now, so
// the client anchors a local countdown to its own clock (avoiding cross-device
// skew). Null means no turn timer is running (manual mode, or the game is over).
@Serializable
@SerialName("TurnTimer")
data class TurnTimer(val remainingSeconds: Int?) : ServerEvent<Nothing, Nothing>

// --- Client-local only ---

// Never sent over the wire — emitted by the client when the WebSocket fails or
// drops, so the UI can leave its loading state.
@Serializable
@SerialName("ConnectionFailed")
data class ConnectionFailed(val reason: String) : ServerEvent<Nothing, Nothing>
