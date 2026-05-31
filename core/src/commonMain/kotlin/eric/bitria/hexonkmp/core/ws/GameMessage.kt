package eric.bitria.hexonkmp.core.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GameMessage

@Serializable
sealed class ServerEvent : GameMessage()

@Serializable
sealed class ClientIntent : GameMessage()

// ---------------------------------------------------------------------------
// Server → Client
// ---------------------------------------------------------------------------

// --- Lobby / matchmaking phase ---

@Serializable
@SerialName("WaitingForPlayers")
data class WaitingForPlayers(val connected: Int, val needed: Int) : ServerEvent()

// Sent once, server-authoritatively, when the room first fills. Also sent to a
// player who reconnects into an already-started game so their UI enters it.
@Serializable
@SerialName("GameStarted")
data object GameStarted : ServerEvent()

// --- In-game events ---
// Game actions/updates (moves, dice, trades, …) belong here as more events are
// added, paired with matching ClientIntent actions.

@Serializable
@SerialName("PlayerJoined")
data class PlayerJoined(val playerId: String) : ServerEvent()

@Serializable
@SerialName("PlayerLeft")
data class PlayerLeft(val playerId: String) : ServerEvent()

// --- Client-local only ---

// Never sent over the wire — emitted by the client when the WebSocket fails or
// drops, so the UI can leave its loading state.
@Serializable
@SerialName("ConnectionFailed")
data class ConnectionFailed(val reason: String) : ServerEvent()
