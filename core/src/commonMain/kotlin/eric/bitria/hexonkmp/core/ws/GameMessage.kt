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

@Serializable
@SerialName("WaitingForPlayers")
data class WaitingForPlayers(val connected: Int, val needed: Int) : ServerEvent()

@Serializable
@SerialName("GameStarted")
data object GameStarted : ServerEvent()

@Serializable
@SerialName("PlayerDisconnected")
data object PlayerDisconnected : ServerEvent()
