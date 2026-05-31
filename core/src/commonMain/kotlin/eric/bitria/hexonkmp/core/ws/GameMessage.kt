package eric.bitria.hexonkmp.core.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class GameMessage

@Serializable
sealed class ServerEvent : GameMessage()

@Serializable
sealed class ClientIntent : GameMessage()

// --- Server → Client ---

// A player connected to the session. `connected`/`needed` let clients still in
// the lobby render the "X/Y" count and derive the game-start transition.
// NOTE: once real game setup exists (deal resources, initial placement, turn
// order) add a separate server-authoritative GameStarted/GameState event rather
// than deriving start from these counts on each client.
@Serializable
@SerialName("PlayerConnected")
data class PlayerConnected(val playerId: String, val connected: Int, val needed: Int) : ServerEvent()

@Serializable
@SerialName("PlayerDisconnected")
data class PlayerDisconnected(val playerId: String, val connected: Int, val needed: Int) : ServerEvent()

// Emitted locally by the client (never sent over the wire) when the
// WebSocket connection fails or drops, so the UI can leave its loading state.
@Serializable
@SerialName("ConnectionFailed")
data class ConnectionFailed(val reason: String) : ServerEvent()
