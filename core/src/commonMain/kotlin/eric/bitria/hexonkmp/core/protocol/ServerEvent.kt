package eric.bitria.hexonkmp.core.protocol

import eric.bitria.hexonkmp.core.game.event.GameEvent
import eric.bitria.hexonkmp.core.game.model.GameState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Server → client envelope. This is the transport protocol: lifecycle messages
// plus a GameUpdate that carries pure domain events. The game engine emits
// GameEvents and never references this type — the server wraps them here.
@Serializable
sealed class ServerEvent

// --- Lobby / matchmaking phase ---

@Serializable
@SerialName("WaitingForPlayers")
data class WaitingForPlayers(val connected: Int, val needed: Int) : ServerEvent()

// Carries the initial snapshot when the room fills, and the current snapshot
// when a player reconnects into an already-running game.
@Serializable
@SerialName("GameStarted")
data class GameStarted(val state: GameState) : ServerEvent()

// --- Presence (in-game) ---

@Serializable
@SerialName("PlayerJoined")
data class PlayerJoined(val playerId: String) : ServerEvent()

@Serializable
@SerialName("PlayerLeft")
data class PlayerLeft(val playerId: String) : ServerEvent()

// --- Game updates ---

// Wraps a domain event produced by the engine's reduce().
@Serializable
@SerialName("GameUpdate")
data class GameUpdate(val event: GameEvent) : ServerEvent()

// Sent only to the acting player when their action failed validation.
@Serializable
@SerialName("ActionRejected")
data class ActionRejected(val reason: String) : ServerEvent()

// --- Client-local only ---

// Never sent over the wire — emitted by the client when the WebSocket fails or
// drops, so the UI can leave its loading state.
@Serializable
@SerialName("ConnectionFailed")
data class ConnectionFailed(val reason: String) : ServerEvent()
