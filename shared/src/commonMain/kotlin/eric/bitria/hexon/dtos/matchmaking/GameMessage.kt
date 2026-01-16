package eric.bitria.hexon.dtos.matchmaking

import kotlinx.serialization.Serializable

@Serializable
sealed class GameMessage {
    abstract val sender: String?

    /** Server to Client: Update on lobby status */
    @Serializable
    data class LobbyUpdate(
        val players: Int,
        val maxPlayers: Int,
        val isReady: Boolean,
        override val sender: String? = "Server"
    ) : GameMessage()

    @Serializable
    data class GameInfo(
        val message: String,
        override val sender: String? = "Server"
    ) : GameMessage()

    /** Client to Server: Example of a message from player */
    @Serializable
    data class PlayerAction(
        val action: String,
        override var sender: String? = null
    ) : GameMessage()
}
