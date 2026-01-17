package eric.bitria.hexon.ws

import kotlinx.serialization.Serializable

@Serializable
sealed class LobbyMessage : GameMessage()
